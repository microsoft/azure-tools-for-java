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

package sample

import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContextWithFailureSave}

case class GenderMeanAge(gender: String, meanAge: Long)
case class People(name: String, age: Int, gender: String)
object AgeMean_Div0 {

  def main(args: Array[String]) {
    val sparkconf = new SparkConf().setAppName("Spark Age Mean")

    val sc = new SparkContextWithFailureSave(sparkconf)
    val spark = SparkSession
      .builder()
      .getOrCreate()

    // For implicit conversions like converting RDDs to DataFrames
    import spark.implicits._
    import org.apache.spark.sql.functions._

    val df = sc.parallelize(Array(
      People("Michael", 20, "M"),
      People("Andy", 34, "M"),
      People("Bob", 28, "M"),
      People("Justin", 30, "F"))).toDF()

    // Show the raw data
    println("The raw data of people:")
    df.show()

    val ageAgg = df.groupBy("gender")
      .agg(sum("age"), count("name"))
      .sort(desc("gender"))

    println("The age sums table group by genders:")
    ageAgg.show()

    println("The age means table group by genders:")
    ageAgg.map(row => {
      val totalAge = row.getAs[Long]("sum(age)")
      // Make a mistake here, the right `peopleCount` should be:
      // val peopleCount = row.getAs[Long]("count(name)")
      val peopleCount = row.getAs[Long]("count(name)") - 1

      GenderMeanAge(row.getString(0), totalAge / peopleCount)
    })
      .show()
  }
}
