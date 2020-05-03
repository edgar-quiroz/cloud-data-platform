package com.quiroz.CloudDataPlatform.Handlers

import com.quiroz.CloudDataPlatform.Models.Trend
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, from_json}

object KafkaSubscriber {
  def getDataframe(sparkSession: SparkSession): Option[DataFrame] ={
    import sparkSession.implicits._
    try{
      Some(
        sparkSession
          .read
          .format("kafka")
          .option("kafka.bootstrap.servers", "kafka:9092")
          .option("subscribe", "trends")
          .load()
          .select(
            from_json(col("value").cast("string"), schema = Trend.schema).as("values")
          )
          .select("values.*")
          .as[Trend]
          .toDF()
      )
    }catch {
      case _: Exception => None
    }
  }
}
