package com.company.edmp

import com.company.edmp.utils.DateHelper.getDate
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * 安徽云指标运算示例
  *
  * 计算每一天每个应用登录用户数
  */
object Demo {

    val PATH_DATA_HOME = "/user/hive/warehouse/s_anhui.db"

    val PATH_LOG_LOGIN = PATH_DATA_HOME + "/log_login"
    val PATH_USER_INFO = PATH_DATA_HOME + "/user_info"
//    val PATH_USER_ORGANIZATION = PATH_DATA_HOME + "/user_organization"

    val PATH_OUT = "/tmp/edmp_tutorial"

    def main(args: Array[String]) {
        val sparkConf = new SparkConf().setAppName("edmp_demo")
        val sc = new SparkContext(sparkConf)
        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits.StringToColumn

        // 从parquet数据源加载数据
        val dfLogLoginSource = sqlContext.read.load(PATH_LOG_LOGIN)
        val dfUserInfoSource = sqlContext.read.load(PATH_USER_INFO)

        // 选择指定列的日志数据
        val dfLogLogin = dfLogLoginSource.select(
            dfLogLoginSource("id").alias("log_id"),
            $"user_id",
            dfLogLoginSource("timestamp").substr(0, 10).alias("date")
        )

        // 选择指定列的用户数据
        val dfUserInfo = dfUserInfoSource.select(
            $"id",
            $"app_name"
        )

        // 联合两个表 (id, app_name, log_id, user_id, date)
        val dfJoin = dfUserInfo.join(dfLogLogin, dfUserInfo("id") === dfLogLogin("user_id"))

        val dfResult = dfJoin.groupBy($"date", $"app_name").count()

        dfResult.write.save(PATH_OUT + "/" + getDate + "/out.parquet")

        sc.stop()
    }
}
