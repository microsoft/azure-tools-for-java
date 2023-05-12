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

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.scheduler.DAGWithFailureSaveScheduler

import scala.collection.immutable
import scala.reflect.ClassTag

class SparkContextWithFailureSave(conf: SparkConf) extends SparkContext(conf) {
  /**
    * Create a SparkContext that loads settings from system properties (for instance, when
    * launching with ./bin/spark-submit).
    */
  def this() = this(new SparkConf())

  /**
    * Alternative constructor that allows setting common Spark properties directly
    *
    * @param master Cluster URL to connect to (e.g. mesos://host:port, spark://host:port, local[4]).
    * @param appName A name for your application, to display on the cluster web UI
    * @param conf a [[org.apache.spark.SparkConf]] object specifying other Spark parameters
    */
  def this(master: String, appName: String, conf: SparkConf) =
    this(SparkContext.updatedConf(conf, master, appName))

  /**
    * Alternative constructor that allows setting common Spark properties directly
    *
    * @param master Cluster URL to connect to (e.g. mesos://host:port, spark://host:port, local[4]).
    * @param appName A name for your application, to display on the cluster web UI.
    * @param sparkHome Location where Spark is installed on cluster nodes.
    * @param jars Collection of JARs to send to the cluster. These can be paths on the local file
    *             system or HDFS, HTTP, HTTPS, or FTP URLs.
    * @param environment Environment variables to set on worker nodes.
    */
  def this(
            master: String,
            appName: String,
            sparkHome: String = null,
            jars: Seq[String] = Nil,
            environment: Map[String, String] = Map()) = {
    this(SparkContext.updatedConf(new SparkConf(), master, appName, sparkHome, jars, environment))
  }

  // NOTE: The below constructors could be consolidated using default arguments. Due to
  // Scala bug SI-8479, however, this causes the compile step to fail when generating docs.
  // Until we have a good workaround for that bug the constructors remain broken out.

  /**
    * Alternative constructor that allows setting common Spark properties directly
    *
    * @param master Cluster URL to connect to (e.g. mesos://host:port, spark://host:port, local[4]).
    * @param appName A name for your application, to display on the cluster web UI.
    */
  private[spark] def this(master: String, appName: String) =
    this(master, appName, null, Nil, Map())

  /**
    * Alternative constructor that allows setting common Spark properties directly
    *
    * @param master Cluster URL to connect to (e.g. mesos://host:port, spark://host:port, local[4]).
    * @param appName A name for your application, to display on the cluster web UI.
    * @param sparkHome Location where Spark is installed on cluster nodes.
    */
  private[spark] def this(master: String, appName: String, sparkHome: String) =
    this(master, appName, sparkHome, Nil, Map())

  /**
    * Alternative constructor that allows setting common Spark properties directly
    *
    * @param master Cluster URL to connect to (e.g. mesos://host:port, spark://host:port, local[4]).
    * @param appName A name for your application, to display on the cluster web UI.
    * @param sparkHome Location where Spark is installed on cluster nodes.
    * @param jars Collection of JARs to send to the cluster. These can be paths on the local file
    *             system or HDFS, HTTP, HTTPS, or FTP URLs.
    */
  private[spark] def this(master: String, appName: String, sparkHome: String, jars: Seq[String]) =
    this(master, appName, sparkHome, jars, Map())

  private[spark] var bcIdMap: immutable.Map[Long, Broadcast[_]] = Map()

  var runtimeFiles: Option[Seq[String]] = Option.empty
  // TODO: Find a way to put the environment listener before the update event is sent
  //
  //  private val sparkEnvListener = new SparkListener {
  //    override def onEnvironmentUpdate(environmentUpdate: SparkListenerEnvironmentUpdate) {
  //      runtimeFiles = environmentUpdate.environmentDetails.get("Classpath Entries").map(pairs => pairs.map(_._1))
  //    }
  //  }
  //
  //  listenerBus.addToEventLogQueue(sparkEnvListener)

  this.dagScheduler = new DAGWithFailureSaveScheduler(this)

  override def broadcast[T: ClassTag](value: T): Broadcast[T] = {
    val bc = super.broadcast(value)

    bcIdMap = bcIdMap + (bc.id -> bc)

    bc
  }
}
