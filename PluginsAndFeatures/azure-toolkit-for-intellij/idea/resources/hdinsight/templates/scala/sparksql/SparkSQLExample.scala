/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package main.scala.sample

import org.apache.spark.sql.Row
// $example on:init_session$
import org.apache.spark.sql.SparkSession
// $example off:init_session$
// $example on:programmatic_schema$
// $example on:data_types$
import org.apache.spark.sql.types._
// $example off:data_types$
// $example off:programmatic_schema$

// Need Spark version >= v2.1
object SparkSQLExample {

  // $example on:create_ds$
  case class Person(name: String, age: Long)
  // $example off:create_ds$

  def main(args: Array[String]) {
    // $example on:init_session$
    val spark = SparkSession
      .builder()
      .appName("Spark SQL basic example")
      .config("spark.some.config.option", "some-value")
      .getOrCreate()

    // For implicit conversions like converting RDDs to DataFrames
    import spark.implicits._
    // $example off:init_session$

    runBasicDataFrameExample(spark)
    runDatasetCreationExample(spark)

    spark.stop()
  }

  private def runBasicDataFrameExample(spark: SparkSession): Unit = {
    // $example on:create_df$
    val df = spark.read.json("wasb:///example/data/people.json")

    // Displays the content of the DataFrame to stdout
    df.show()
    // +---+-----+
    // |age| name|
    // +---+-----+
    // | 22|Ricky|
    // | 36| Jeff|
    // | 62|Geddy|
    // +---+-----+
    // $example off:create_df$

    // $example on:untyped_ops$
    // This import is needed to use the $-notation
    import spark.implicits._
    // Print the schema in a tree format
    df.printSchema()
    // root
    // |-- age: long (nullable = true)
    // |-- name: string (nullable = true)

    // Select only the "name" column
    df.select("name").show()
    // +-----+
    // | name|
    // +-----+
    // |Ricky|
    // | Jeff|
    // |Geddy|
    // +-----+

    // Select everybody, but increment the age by 1
    df.select($"name", $"age" + 1).show()
    // +-----+---------+
    // | name|(age + 1)|
    // +-----+---------+
    // |Ricky|       23|
    // | Jeff|       37|
    // |Geddy|       63|
    // +-----+---------+

    // Select people older than 40
    df.filter($"age" > 40).show()
    // +---+-----+
    // |age| name|
    // +---+-----+
    // | 62|Geddy|
    // +---+-----+

    // Count people by age
    df.groupBy("age").count().show()
    // +---+-----+
    // |age|count|
    // +---+-----+
    // | 22|    1|
    // | 62|    1|
    // | 36|    1|
    // +---+-----+
    // $example off:untyped_ops$

    // $example on:run_sql$
    // Register the DataFrame as a SQL temporary view
    df.createOrReplaceTempView("people")

    val sqlDF = spark.sql("SELECT * FROM people")
    sqlDF.show()
    // +---+-----+
    // |age| name|
    // +---+-----+
    // | 22|Ricky|
    // | 36| Jeff|
    // | 62|Geddy|
    // +---+-----+
    // $example off:run_sql$

    // $example on:global_temp_view$
    // Register the DataFrame as a global temporary view
    df.createGlobalTempView("people")

    // Global temporary view is tied to a system preserved database `global_temp`
    spark.sql("SELECT * FROM global_temp.people").show()
    // +---+-----+
    // |age| name|
    // +---+-----+
    // | 22|Ricky|
    // | 36| Jeff|
    // | 62|Geddy|
    // +---+-----+

    // Global temporary view is cross-session
    spark.newSession().sql("SELECT * FROM global_temp.people").show()
    // +---+-----+
    // |age| name|
    // +---+-----+
    // | 22|Ricky|
    // | 36| Jeff|
    // | 62|Geddy|
    // +---+-----+
    // $example off:global_temp_view$
  }

  private def runDatasetCreationExample(spark: SparkSession): Unit = {
    import spark.implicits._
    // $example on:create_ds$
    // Encoders are created for case classes
    val caseClassDS = Seq(Person("Andy", 32)).toDS()
    caseClassDS.show()
    // +----+---+
    // |name|age|
    // +----+---+
    // |Andy| 32|
    // +----+---+

    // Encoders for most common types are automatically provided by importing spark.implicits._
    val primitiveDS = Seq(1, 2, 3).toDS()
    primitiveDS.map(_ + 1).collect() // Returns: Array(2, 3, 4)

    // DataFrames can be converted to a Dataset by providing a class. Mapping will be done by name
    val path = "wasb:///example/data/people.json"
    val peopleDS = spark.read.json(path).as[Person]
    peopleDS.show()
    // +---+-----+
    // |age| name|
    // +---+-----+
    // | 22|Ricky|
    // | 36| Jeff|
    // | 62|Geddy|
    // +---+-----+
    // $example off:create_ds$
  }
}