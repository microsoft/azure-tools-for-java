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

package org.apache.spark

import java.io._
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util.Base64

import com.google.common.io.ByteStreams
import org.apache.hadoop.fs.Path
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.executor.TaskMetrics
import org.apache.spark.internal.Logging
import org.apache.spark.network.buffer.FileSegmentManagedBuffer
import org.apache.spark.scheduler._
import org.apache.spark.serializer.SerializerInstance
import org.apache.spark.storage.{BlockId, ShuffleIndexBlockId}

import scala.reflect.ClassTag

class TaskRecovery(sc: SparkContext, failureTask: FailureTask) extends Logging {
  import TaskRecovery._

  private val serializer = SparkEnv.get.closureSerializer.newInstance()
  private val fs = org.apache.hadoop.fs.FileSystem.get(sc.hadoopConfiguration)
  private val bcMap: Map[Long, Broadcast[Any]] = failureTask.bcs
    .sortBy(_.id)
    .map(bc => {
      val newBc = sc.broadcast(decodeObj[Any](serializer, bc.value))

      bc.id -> newBc
    }) toMap

  private val taskBinary = bcMap(failureTask.binaryTaskBcId).asInstanceOf[Broadcast[Array[Byte]]]

  private val part = decodeObj[Partition](serializer, failureTask.partitionEnc)
  private val locs = failureTask.hosts.map(HostTaskLocation).toSeq
  private val metrics = TaskMetrics.registered


  val recoveredTask: Task[_ <: MapStatus] = if (failureTask.isResult) {
    failureTask.shuffleDeps.foreach(shuffleDep => {
      shuffleDep.shuffleData.foreach { shuffleData => {
        val blockId = BlockId(shuffleData.name)
        val SHUFFLE = "shuffle_([0-9]+)_([0-9]+)_([0-9]+)".r

        blockId.toString() match {
          case SHUFFLE(shuffleId, mapId, reduceId) =>
            val shuffleIndexId = ShuffleIndexBlockId(shuffleId.toInt, mapId.toInt, 0)

            val idxFile = SparkEnv.get.blockManager.diskBlockManager.getFile(shuffleIndexId)
            Files.copy(
              Paths.get(idxFile.getName),
              idxFile.toPath,
              StandardCopyOption.REPLACE_EXISTING)

            SparkEnv.get.blockManager.getBlockData(blockId) match {
              case fileSegMgtBuf: FileSegmentManagedBuffer =>
                val segFile = fileSegMgtBuf.getFile

                val shuffleSavedFile = new Path(shuffleData.path)

                val in = fs.open(shuffleSavedFile)

                importShuffleToLocal(
                  fileSegMgtBuf.getOffset,
                  fileSegMgtBuf.getLength,
                  reduceId.toInt,
                  in,
                  segFile
                )

                logInfo(s"Block file ${shuffleSavedFile.getName} imported for " +
                  s"Shuffle ID $shuffleId, Map ID $mapId, Reduce ID $reduceId")

                in.close()
            }
          case _ =>
        }

        }}

        val recoveryMapStatus = MapOutputTracker.deserializeMapStatuses(
          Base64.getDecoder.decode(shuffleDep.mapStatusEnc))
          .map(loc => loc.getClass.getDeclaredFields
            .find(_.getName.endsWith("$$loc"))
            .map(locationField => {
              locationField.setAccessible(true)
              locationField.set(loc, SparkEnv.get.blockManager.shuffleServerId)

              loc
            })
            .getOrElse(loc))

        SparkEnv.get.mapOutputTracker match {
          case trackerMaster: MapOutputTrackerMaster =>
            // Register shuffle firstly
            trackerMaster.registerShuffle(shuffleDep.id, recoveryMapStatus.size)

            recoveryMapStatus.view.zipWithIndex.foreach { case (mapStatus, index) =>
              trackerMaster.registerMapOutput(
                shuffleDep.id,
                index,
                mapStatus)
            }
        }
    })

    new ResultTask(
      failureTask.stageId,
      failureTask.stageAttemptId,
      taskBinary,
      part,
      locs,
      0,
      failureTask.localProperties,
      serializer.serialize(metrics).array()
    )
  } else {
    new ShuffleMapTask(
      failureTask.stageId,
      failureTask.stageAttemptId,
      taskBinary,
      part,
      locs,
      failureTask.localProperties,
      serializer.serialize(metrics).array()
    )
  }

  def rerun(): Unit = {
    val rerunTaskSet = new TaskSet(
      Array(recoveredTask),
      failureTask.stageId,
      failureTask.stageAttemptId,
      0,
      failureTask.localProperties
    )
    sc.taskScheduler.submitTasks(rerunTaskSet)
  }
}

object TaskRecovery {
  def decodeObj[T : ClassTag](serializer: SerializerInstance, code: String): T = {
    val objBytes = Base64.getDecoder.decode(code)

    serializer.deserialize[T](ByteBuffer.wrap(objBytes))
  }

  def importShuffleToLocal(offset: Long,
                           len: Long,
                           reduceId: Int,
                           dataIn: InputStream,
                           outDataFile: File): Unit = {
    val dataOut = new BufferedOutputStream(new FileOutputStream(outDataFile))

    0.toLong.until(offset).foreach(_ => dataOut.write(20))  // writing SPACE for padding

    ByteStreams.copy(dataIn, dataOut)

    dataOut.close()
  }

  def rerun(sc: SparkContext, failureTask: FailureTask): Unit = {
    val recoveryTask = new TaskRecovery(sc, failureTask)
    recoveryTask.rerun()

    while (sc.taskScheduler.rootPool.schedulableQueue.size() != 0) {
      Thread.sleep(1000)
    }
  }

}
