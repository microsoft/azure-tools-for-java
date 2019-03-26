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

import java.util.Properties

import org.apache.spark.executor.TaskMetrics

case class BroadcastValue(
  id: Long,
  value: String
)

case class ShuffleData(
  name: String,
  path: String,
  location: String
)
case class ShuffleDeps(
  id: Int,
  shuffleData: Array[ShuffleData],
  mapStatusEnc: String
)

case class FailureTask(
  binaryTaskBcId: Long,
  taskId: String,
  name: String,
  stageId: Int,
  stageAttemptId: Int,
  partitionEnc: String,
  hosts: Array[String],
  outputId: Int,
  localProperties: Properties,
  metrics: TaskMetrics,
//  conf: String,
  bcs: Array[BroadcastValue],
  shuffleDeps: Array[ShuffleDeps],
  isResult: Boolean
)

