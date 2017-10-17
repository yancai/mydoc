#!/usr/bin/python
# -*- coding:utf-8 -*-
# Filename: 'demo.py'
# Author:   'yancai'
# Date:     '2016/7/6'

"""Documentation"""
from pyspark import SparkContext
from pyspark.sql import SQLContext
from pyspark.sql.functions import col


PATH_LOG_LOGIN = "/user/hive/warehouse/s_anhui.db/log_login"
PATH_USER_INFO = "/user/hive/warehouse/s_anhui.db/user_info"

PATH_OUT = "/tmp/edmp_tutorial/pyspark/out.parquet"


def tutorial_demo():
    sc = SparkContext(appName="py_edmp_demo")
    sql_context = SQLContext(sc)

    df_log_login_source = sql_context.read.load(PATH_LOG_LOGIN)
    df_user_info_source = sql_context.read.load(PATH_USER_INFO)

    df_log_login = df_log_login_source.select(
        df_log_login_source["id"].alias("log_id"),
        col("user_id"),
        df_log_login_source["timestamp"][0:10].alias("date")
    )

    df_user_info = df_user_info_source.select(
        "id",
        col("app_name")
    )

    df_join = df_user_info.join(
        df_log_login, df_user_info["id"] == df_log_login["user_id"]
    )

    df_result = df_join.groupBy("date", "app_name").count()

    df_result.write.save(PATH_OUT)

    sc.stop()


if __name__ == "__main__":
    tutorial_demo()
    pass
