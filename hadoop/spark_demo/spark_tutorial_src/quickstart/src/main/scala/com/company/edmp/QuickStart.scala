package com.company.edmp

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * QuickStart类
  *
  */
object QuickStart {

    def main(args: Array[String]) {
        val sparkConf = new SparkConf().setAppName("edmp_quickstart")
        val sc = new SparkContext(sparkConf)
        val sqlContext = new SQLContext(sc)

        import sqlContext.implicits._

        val dfUser = sqlContext.createDataFrame(Seq(
            ("1", "tom", 21),
            ("2", "jack", 27),
            ("3", "lucy", 30)
        )).toDF("id", "name", "age")

        val result = dfUser.filter($"age" > 25).count()
        println("==============")
        println(result)
        println("==============")

        sc.stop()
    }

}
