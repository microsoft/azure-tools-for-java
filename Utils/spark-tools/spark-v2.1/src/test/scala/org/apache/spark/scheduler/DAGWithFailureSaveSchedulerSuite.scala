/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.apache.spark.scheduler

import java.util.Properties

import org.apache.spark._
import org.apache.spark.broadcast.BroadcastManager
import org.apache.spark.rdd.RDD
import org.apache.spark.scheduler.SchedulingMode.SchedulingMode
import org.apache.spark.storage.{BlockId, BlockManagerId, BlockManagerMaster}
import org.apache.spark.util.{AccumulatorV2, CallSite}
import org.scalatest.concurrent.{Signaler, ThreadSignaler, TimeLimits}

import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet, Map}

class DAGWithFailureSaveSchedulerSuite extends SparkFunSuite with LocalSparkContext with TimeLimits {
  // Borrow the context preparing codes from org.apache.spark.scheduler.DAGSchedulerSuite

  import DAGSchedulerSuite._

  // Necessary to make ScalaTest 3.x interrupt a thread on the JVM like ScalaTest 2.2.x
  implicit val defaultSignaler: Signaler = ThreadSignaler

  val conf = new SparkConf
  /** Set of TaskSets the DAGScheduler has requested executed. */
  val taskSets = scala.collection.mutable.Buffer[TaskSet]()

  /** Stages for which the DAGScheduler has called TaskScheduler.cancelTasks(). */
  val cancelledStages = new HashSet[Int]()

  /** Length of time to wait while draining listener events. */
  val WAIT_TIMEOUT_MILLIS = 10000
  val sparkListener = new SparkListener() {
    val submittedStageInfos = new HashSet[StageInfo]
    val successfulStages = new HashSet[Int]
    val failedStages = new ArrayBuffer[Int]
    val stageByOrderOfExecution = new ArrayBuffer[Int]
    val endedTasks = new HashSet[Long]

    override def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted) {
      submittedStageInfos += stageSubmitted.stageInfo
    }

    override def onStageCompleted(stageCompleted: SparkListenerStageCompleted) {
      val stageInfo = stageCompleted.stageInfo
      stageByOrderOfExecution += stageInfo.stageId
      if (stageInfo.failureReason.isEmpty) {
        successfulStages += stageInfo.stageId
      } else {
        failedStages += stageInfo.stageId
      }
    }

    override def onTaskEnd(taskEnd: SparkListenerTaskEnd): Unit = {
      endedTasks += taskEnd.taskInfo.taskId
    }
  }

  var mapOutputTracker: MapOutputTrackerMaster = null
  var broadcastManager: BroadcastManager = null
  var securityMgr: SecurityManager = null
  var scheduler: DAGWithFailureSaveScheduler = null
  //var dagEventProcessLoopTester: DAGSchedulerEventProcessLoop = null
  val taskScheduler = new TaskScheduler() {
    override def schedulingMode: SchedulingMode = SchedulingMode.FIFO
    override def rootPool: Pool = new Pool("", schedulingMode, 0, 0)
    override def start() = {}
    override def stop() = {}
    override def executorHeartbeatReceived(
                                            execId: String,
                                            accumUpdates: Array[(Long, Seq[AccumulatorV2[_, _]])],
                                            blockManagerId: BlockManagerId): Boolean = true
    override def submitTasks(taskSet: TaskSet) = {
      // normally done by TaskSetManager
      taskSet.tasks.foreach(_.epoch = mapOutputTracker.getEpoch)
      taskSets += taskSet
    }
    override def cancelTasks(stageId: Int, interruptThread: Boolean) {
      cancelledStages += stageId
    }
//    override def killTaskAttempt(
//                                  taskId: Long, interruptThread: Boolean, reason: String): Boolean = false
    override def setDAGScheduler(dagScheduler: DAGScheduler) = {}
    override def defaultParallelism() = 2
    override def executorLost(executorId: String, reason: ExecutorLossReason): Unit = {}
//    override def workerRemoved(workerId: String, host: String, message: String): Unit = {}
    override def applicationAttemptId(): Option[String] = None
  }

  /**
    * Set of cache locations to return from our mock BlockManagerMaster.
    * Keys are (rdd ID, partition ID). Anything not present will return an empty
    * list of cache locations silently.
    */
  val cacheLocations = new HashMap[(Int, Int), Seq[BlockManagerId]]

  var dagEventProcessLoopTester: DAGSchedulerEventProcessLoop = null

  val blockManagerMaster = new BlockManagerMaster(null, conf, true) {
    override def getLocations(blockIds: Array[BlockId]): IndexedSeq[Seq[BlockManagerId]] = {
      blockIds.map {
        _.asRDDId.map(id => (id.rddId -> id.splitIndex)).flatMap(key => cacheLocations.get(key)).
          getOrElse(Seq())
      }.toIndexedSeq
    }
    override def removeExecutor(execId: String) {
      // don't need to propagate to the driver, which we don't have
    }
  }

  /** The list of results that DAGScheduler has collected. */
  val results = new HashMap[Int, Any]()
  var failure: Exception = _
  val jobListener = new JobListener() {
    override def taskSucceeded(index: Int, result: Any) = results.put(index, result)
    override def jobFailed(exception: Exception) = { failure = exception }
  }

  def mySC: SparkContextWithFailureSave = sc.asInstanceOf[SparkContextWithFailureSave]

  override def beforeEach(): Unit = {
    super.beforeEach()
    init(new SparkConf())
  }

  private def init(testConf: SparkConf): Unit = {
    sc = new SparkContextWithFailureSave("local", "DAGWithFailureSaveSchedulerSuite", testConf)
    sparkListener.submittedStageInfos.clear()
    sparkListener.successfulStages.clear()
    sparkListener.failedStages.clear()
    sparkListener.endedTasks.clear()
    failure = null
    sc.addSparkListener(sparkListener)
    taskSets.clear()
    cancelledStages.clear()
    cacheLocations.clear()
    results.clear()
    broadcastManager = new BroadcastManager(true, conf, securityMgr)
    mapOutputTracker = new MapOutputTrackerMaster(conf, broadcastManager, true) {
      override def sendTracker(message: Any): Unit = {
        // no-op, just so we can stop this to avoid leaking threads
      }
    }

    scheduler = new DAGWithFailureSaveScheduler(
      mySC,
      taskScheduler,
      mySC.listenerBus,
      mapOutputTracker,
      blockManagerMaster,
      mySC.env)

    dagEventProcessLoopTester = new DAGSchedulerEventProcessLoopTester(scheduler)
  }

  override def afterEach(): Unit = {
    try {
      scheduler.stop()
      dagEventProcessLoopTester.stop()
      mapOutputTracker.stop()
      broadcastManager.stop()
    } finally {
      super.afterEach()
    }
  }

  /**
    * When we submit dummy Jobs, this is the compute function we supply. Except in a local test
    * below, we do not expect this function to ever be executed; instead, we will return results
    * directly through CompletionEvents.
    */
  private val jobComputeFunc = (context: TaskContext, it: Iterator[(_)]) =>
    it.next.asInstanceOf[Tuple2[_, _]]._1

  /**
    * Process the supplied event as if it were the top of the DAGScheduler event queue, expecting
    * the scheduler not to exit.
    *
    * After processing the event, submit waiting stages as is done on most iterations of the
    * DAGScheduler event loop.
    */
  private def runEvent(event: DAGSchedulerEvent) {
    dagEventProcessLoopTester.post(event)
  }

  /** Send the given CompletionEvent messages for the tasks in the TaskSet. */
  private def complete(taskSet: TaskSet, results: Seq[(TaskEndReason, Any)]) {
    assert(taskSet.tasks.size >= results.size)
    for ((result, i) <- results.zipWithIndex) {
      if (i < taskSet.tasks.size) {
        runEvent(makeCompletionEvent(taskSet.tasks(i), result._1, result._2))
      }
    }
  }

  // Nothing in this test should break if the task info's fields are null, but
  // OutputCommitCoordinator requires the task info itself to not be null.
  private def createFakeTaskInfo(): TaskInfo = {
    val info = new TaskInfo(0, 0, 0, 0L, "", "", TaskLocality.ANY, false)
    info.finishTime = 1
    info
  }

  private def makeCompletionEvent(
                                   task: Task[_],
                                   reason: TaskEndReason,
                                   result: Any,
                                   extraAccumUpdates: Seq[AccumulatorV2[_, _]] = Seq.empty,
                                   taskInfo: TaskInfo = createFakeTaskInfo()): CompletionEvent = {
    val accumUpdates = reason match {
      case Success => task.metrics.accumulators()
      case ef: ExceptionFailure => ef.accums
      case _ => Seq.empty
    }
    CompletionEvent(task, reason, result, accumUpdates ++ extraAccumUpdates, taskInfo)
  }

  /** Submits a job to the scheduler and returns the job id. */
  private def submit(
                      rdd: RDD[_],
                      partitions: Array[Int],
                      func: (TaskContext, Iterator[_]) => _ = jobComputeFunc,
                      listener: JobListener = jobListener,
                      properties: Properties = null): Int = {
    val jobId = scheduler.nextJobId.getAndIncrement()
    runEvent(JobSubmitted(jobId, rdd, func, partitions, CallSite("", ""), listener, properties))
    jobId
  }

  /** Sends TaskSetFailed to the scheduler. */
  private def failed(taskSet: TaskSet, message: String) {
    runEvent(TaskSetFailed(taskSet, message, None))
  }

  private def assertDataStructuresEmpty(): Unit = {
    assert(scheduler.activeJobs.isEmpty)
    assert(scheduler.failedStages.isEmpty)
    assert(scheduler.jobIdToActiveJob.isEmpty)
    assert(scheduler.jobIdToStageIds.isEmpty)
    assert(scheduler.stageIdToStage.isEmpty)
    assert(scheduler.runningStages.isEmpty)
    assert(scheduler.shuffleIdToMapStage.isEmpty)
    assert(scheduler.waitingStages.isEmpty)
    assert(scheduler.outputCommitCoordinator.isEmpty)
  }

  test("run trivial shuffle") {
    val shuffleMapRdd = new MyRDD(sc, 2, Nil)
    val shuffleDep = new ShuffleDependency(shuffleMapRdd, new HashPartitioner(1))
    val shuffleId = shuffleDep.shuffleId
    val reduceRdd = new MyRDD(sc, 1, List(shuffleDep), tracker = mapOutputTracker)
    submit(reduceRdd, Array(0))
    complete(taskSets(0), Seq(
      (Success, makeMapStatus("hostA", 1)),
      (Success, makeMapStatus("hostB", 1))))
    assert(mapOutputTracker.getMapSizesByExecutorId(shuffleId, 0).map(_._1).toSet ===
      HashSet(makeBlockManagerId("hostA"), makeBlockManagerId("hostB")))
    complete(taskSets(1), Seq((Success, 42)))
    assert(results === Map(0 -> 42))
    assertDataStructuresEmpty()
  }

  test("trivial job failure") {
    submit(new MyRDD(sc, 1, Nil), Array(0))
    failed(taskSets(0), "some failure")
    assert(failure.getMessage === "Job aborted due to stage failure: some failure")
    sc.listenerBus.waitUntilEmpty(WAIT_TIMEOUT_MILLIS)
    assert(sparkListener.failedStages.contains(0))
    assert(sparkListener.failedStages.size === 1)
    assertDataStructuresEmpty()
  }
}
