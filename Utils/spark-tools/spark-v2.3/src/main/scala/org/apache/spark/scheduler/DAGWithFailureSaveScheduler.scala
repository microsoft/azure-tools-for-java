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

import java.io._
import java.text.SimpleDateFormat
import java.util.{Base64, Date}

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.fs.{FileUtil, Path}
import org.apache.spark._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.network.buffer.ManagedBuffer
import org.apache.spark.rdd.RDD
import org.apache.spark.storage._
import org.apache.spark.util.{Clock, SystemClock, Utils}
import org.json4s.jackson.Serialization.write

import scala.collection.mutable
import scala.language.postfixOps
import scala.util.control.NonFatal

private[spark]
class DAGWithFailureSaveScheduler(
    sc: SparkContextWithFailureSave,
    taskScheduler: TaskScheduler,
    listenerBus: LiveListenerBus,
    var mapOutputTracker: MapOutputTrackerMaster,
    blockManagerMaster: BlockManagerMaster,
    env: SparkEnv,
    clock: Clock = new SystemClock())
  extends DAGScheduler(sc, taskScheduler, listenerBus, mapOutputTracker, blockManagerMaster, env, clock) {

  def this(sc: SparkContextWithFailureSave, taskScheduler: TaskScheduler) = {
    this(
      sc,
      taskScheduler,
      sc.listenerBus,
      sc.env.mapOutputTracker.asInstanceOf[MapOutputTrackerMaster],
      sc.env.blockManager.master,
      sc.env)
  }

  def this(sc: SparkContextWithFailureSave) = this(sc, sc.taskScheduler)

  var jobRDD: RDD[AnyRef] = _
  private val driverBlockManager = SparkEnv.get.blockManager
//  private val mapOutputTracker = sc.env.mapOutputTracker.asInstanceOf[MapOutputTrackerMaster]
  val failedEvents: mutable.HashMap[Int, CompletionEvent] = new mutable.HashMap()
  private val wd = org.apache.hadoop.fs.FileSystem.get(sc.hadoopConfiguration).getWorkingDirectory
  private val failureEventsDir = sc.eventLogDir.getOrElse(wd.toUri)
  private val fs = Utils.getHadoopFileSystem(failureEventsDir, sc.hadoopConfiguration)
  private val minSizeForBroadcast =
    sc.conf.getSizeAsBytes("spark.shuffle.mapOutput.minSizeForBroadcast", "512k").toInt

  def getEncodedByteArray(buffer: Array[Byte]): String =
    Base64.getEncoder.encode(buffer)
      .map(_.toChar)
      .mkString

  def encodeObject(obj: Any): String = {
    val bytesOutputStream = new ByteArrayOutputStream()
    val objOutputStream = new ObjectOutputStream(bytesOutputStream)
    objOutputStream.writeObject(obj)
    objOutputStream.close()

    getEncodedByteArray(bytesOutputStream.toByteArray)
  }

  def writeIndexFile(outputStream: OutputStream, lengths: Array[Long]): Unit = {
    val out = new DataOutputStream(new BufferedOutputStream(outputStream))
    Utils.tryWithSafeFinally {
      // We take in lengths of each block, need to convert it to offsets.
      var offset = 0L
      out.writeLong(offset)
      for (length <- lengths) {
        offset += length
        out.writeLong(offset)
      }
    } {
      out.close()
    }
  }


  // from remote or local
  def getShuffleBuffer(blockManagerId: BlockManagerId, blockId: BlockId): (BlockId, Option[ManagedBuffer]) = {
    logDebug(s"Getting shuffle block $blockId from $blockManagerId")

    try {
      val buffer = driverBlockManager.blockTransferService.fetchBlockSync(
        blockManagerId.host, blockManagerId.port, blockManagerId.executorId, blockId.toString(), null)

      (blockId, Some(buffer))
    } catch {
      case NonFatal(e) =>
        logWarning(s"Failed to fetch remote block $blockId from $blockManagerId, failure cause:", e)

        (blockId, None)
    }
  }

  def saveFailureTask(task: Task[_], stageId: Int, taskId: String, attemptId: Int, timestamp: String): Path = {
    def getFailureSavingPath(fileName: String = null): Path = {
      val appFolderName = sc.applicationId + sc.applicationAttemptId.map(attemptId => s"_attempt_${attemptId}_").getOrElse("_") + timestamp
      
      val savingBase: Path = new Path(new Path(new Path(failureEventsDir), ".spark-failures"), appFolderName)

      if (fileName != null) {
        new Path(savingBase, fileName)
      } else {
        savingBase
      }
    }

    // Save task binary ID in broadcasts
    val taskBinaryField = task.getClass.getDeclaredField("taskBinary")
    taskBinaryField.setAccessible(true)
    val taskBinaryBc = taskBinaryField.get(task).asInstanceOf[Broadcast[Array[Byte]]]
    val taskBinaryBcId = taskBinaryBc.id

    // Save partition infos
    val partitionField = task.getClass.getDeclaredField("partition")
    partitionField.setAccessible(true)
    val partition = partitionField.get(task).asInstanceOf[Partition]

    // Find the failed stage
    val failedStage = stageIdToStage(stageId)
    val shuffleMgrBlockIds = failedStage.parents.map {
      case shuffleStage: ShuffleMapStage => (
        shuffleStage.shuffleDep.shuffleId,
        mapOutputTracker.getMapSizesByExecutorId(shuffleStage.shuffleDep.shuffleId, partition.index)
      )
    }


    val shuffleDeps = shuffleMgrBlockIds.flatMap { case (shuffleId, mgrBlockIds) =>
      mgrBlockIds
        .map { case (blockMgrId, blockIds) =>
          logInfo(blockMgrId.toString())

          val mapStatus = mapOutputTracker.shuffleStatuses.get(shuffleId).head
            .serializedMapStatus(SparkEnv.get.broadcastManager, sc.isLocal, minSizeForBroadcast)

          ShuffleDeps(
            shuffleId,
            blockIds
              .filter(_._2 > 0)
              .map { case (blockId, blockSize) =>
                getShuffleBuffer(blockMgrId, blockId)._2 match {
                  case Some(buffer: ManagedBuffer) =>
                    // Copy the shuffle partition data into a file
                    val shuffleFile = getFailureSavingPath(blockId.toString())
                    val shuffleIn = buffer.createInputStream()

                    logInfo(s"Generate shuffle files: $shuffleFile")
                    val shuffleOut = fs.create(shuffleFile, true)

                    IOUtils.copy(shuffleIn, shuffleOut)

                    buffer.release()
                    shuffleIn.close()
                    shuffleOut.close()

                    // Need to prepare the index file for recovering
                    val SHUFFLE = "shuffle_([0-9]+)_([0-9]+)_([0-9]+)".r
                    blockId.toString() match {
                      case SHUFFLE(shuffleId, mapId, reduceId) =>
                        val shuffleIndexId = ShuffleIndexBlockId(shuffleId.toInt, mapId.toInt, 0)
                        val idxFile = driverBlockManager.diskBlockManager.getFile(shuffleIndexId).getName
                        val idxFileOutput = fs.create(getFailureSavingPath(idxFile), true)

                        writeIndexFile(idxFileOutput, Array.fill(reduceId.toInt)(0.toLong) :+ blockSize)
                        idxFileOutput.close()
                    }

                    ShuffleData(blockId.toString, blockId.toString, blockMgrId.toString())
                }
              } toArray,
            getEncodedByteArray(mapStatus))
        }
    } filter(_.shuffleData.nonEmpty) toArray

    // Get broadcast values
    val bcs = sc.bcIdMap.map { case (id, bc) =>
      BroadcastValue(id, encodeObject(bc.value))
    } toArray

    implicit val formats = org.json4s.DefaultFormats

    val taskName = s"task ${taskId} in stage ${task.stageId}"
    val failureTask = FailureTask(
      taskBinaryBcId,
      taskId,
      taskName,
      stageId,
      attemptId,
      encodeObject(partition),
      Array(),
      -1,
      task.localProperties,
      task.metrics,
      bcs,
      shuffleDeps,
      task.isInstanceOf[ResultTask[Any, Any]]
    )

    // Serialize to JSON
    val json = write(failureTask)
    val failureContextFile = getFailureSavingPath("failure_save.ftd")
    val out = fs.create(failureContextFile, true)
    val writer = new PrintWriter(out)

    writer.write(json)
    writer.close()

    logInfo(s"The working directory is ${fs.getWorkingDirectory.toUri}")
    logInfo("Failure task has been saved into " + failureContextFile.getParent)

    // Save runtime files
    val runtimeFolder = getFailureSavingPath("runtime/")
    sc.runtimeFiles
      .orElse({
        val schedulingMode = sc.getSchedulingMode.toString
        val addedJarPaths = sc.addedJars.keys.toSeq
        val addedFilePaths = sc.addedFiles.keys.toSeq
        val environmentDetails = SparkEnv.environmentDetails(sc.conf, schedulingMode, addedJarPaths,
          addedFilePaths)

        environmentDetails.get("Classpath Entries").map(pairs => pairs.map(_._1))
      })
      .foreach(_.par.foreach(runtimeFile => {
        try {
          val srcUri = Utils.resolveURI(runtimeFile)
          val srcPath = new Path(srcUri)
          val srcFs = Utils.getHadoopFileSystem(srcUri, sc.hadoopConfiguration)

          if (StringUtils.isNotBlank(srcPath.getName)) {
            val dstPath = new Path(runtimeFolder, srcPath.getName)
            FileUtil.copy(srcFs, srcPath, fs, dstPath, false, sc.hadoopConfiguration)
            logInfo(s"Runtime $srcPath has been saved into $dstPath")
          } else {
            logWarning(s"Runtime $runtimeFile has been ignored")
          }
        } catch {
          case NonFatal(err) => logWarning(s"Got an error when saving runtime $runtimeFile", err)
        }
      }))

    failureContextFile.getParent
  }

  override private[scheduler] def handleTaskCompletion(event: CompletionEvent): Unit = {
    val task = event.task
    val taskId = event.taskInfo.id
    val stageId = task.stageId
    //    val taskType = Utils.getFormattedClassName(task)

    super.handleTaskCompletion(event)

    event.reason match {
      case exceptionFailure: ExceptionFailure =>
        failedEvents(event.taskInfo.index) = event
      //        saveFailureTask(task, stageId, taskId, event.taskInfo.attemptNumber)
      case Success =>
        failedEvents.remove(event.taskInfo.index)
      case _ =>
    }
  }

  override private[scheduler] def handleTaskSetFailed(taskSet: TaskSet, reason: String, exception: Option[Throwable]): Unit = {
    val FailureMessageRegEx = """(?s)^Task (\d+) in stage (.+) failed (\d+) times.*""".r
    val reasonWithFailureDebug: String = reason match {
      case FailureMessageRegEx(taskIndex, stage, retries) => {
        failedEvents.get(taskIndex.toInt)
          .map(event => {
            val task = event.task
            val taskId = event.taskInfo.id
            val stageId = task.stageId
            val taskFailureTimestamp = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'").format(new Date(event.taskInfo.finishTime))

            logInfo("Save failure task " + taskIndex)
            val savedFolder = saveFailureTask(task, stageId, taskId, event.taskInfo.attemptNumber, taskFailureTimestamp)

            val driverStacktraceStart = reason.indexOf("Driver stacktrace:")
            val insertPlace = if (driverStacktraceStart < 0)
              reason.size
            else
              driverStacktraceStart

            s"""
              |${reason.substring(0, insertPlace)}Failure debugging:
              |    Failure context saved into $savedFolder
              |${reason.substring(insertPlace)}""".stripMargin
          })
          .getOrElse(reason)
      }
      case _ => reason
    }

    super.handleTaskSetFailed(taskSet, reasonWithFailureDebug, exception)
  }
}
