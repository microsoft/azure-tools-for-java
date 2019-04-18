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

package org.apache.spark.tools

import org.apache.spark.{FailureTask, SparkConf, SparkContext, TaskRecovery}
import org.json4s.jackson.Serialization.read

object FailureTaskRecoveryApp {
  def main(args: Array[String]): Unit = {
    val sparkconf = new SparkConf().setAppName("Failure task recovery").setMaster("local[1]")

    val sc = new SparkContext(sparkconf)

    implicit val formats = org.json4s.DefaultFormats

    val failureTaskContextFile = sparkconf.get("spark.failure.task.context")
    val source = scala.io.Source.fromFile(failureTaskContextFile)
    val json = source.mkString
    source.close()

    val failureTask = read[FailureTask](json)

    TaskRecovery.rerun(sc, failureTask)
  }

}
