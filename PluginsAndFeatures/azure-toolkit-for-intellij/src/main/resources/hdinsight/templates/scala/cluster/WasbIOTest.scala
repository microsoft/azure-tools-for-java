import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

/**
  * Test IO to wasb
  */
object WasbIOTest {
  def main (arg: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("WASBIOTest")
    val sc = new SparkContext(conf)

    val rdd = sc.textFile("wasb:///HdiSamples/HdiSamples/SensorSampleData/hvac/HVAC.csv")

    // find the rows which have only one digit in the 6th column
    val rdd1 = rdd.filter(s => s.split(",")(6).length() == 1)

    rdd1.saveAsTextFile("wasb:///HVACout2")
  }
}