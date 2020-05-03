package com.quiroz.CloudDataPlatform

import java.util.logging.Logger

import com.quiroz.CloudDataPlatform.Handlers.{DataframeWriter, DeltaHandler, KafkaSubscriber}
import com.quiroz.CloudDataPlatform.SparkSession.SparkBuilder

class TrendsConsumer {
  private val appName = "trends_consumer"
  private val logger = Logger.getLogger(appName)
  private val bucket = "s3a://your-bucket-here"
  private val s3DailyBucket = s"$bucket/daily/"
  private val accessKey = "your-access-here"
  private val secretKey = "yout-secret-here"

  private val sparkSession = SparkBuilder.getSession(appName)
  sparkSession.conf.set("fs.s3a.access.key", accessKey)
  sparkSession.conf.set("fs.s3a.secret.key", secretKey)

  def startJob(): Unit ={
    KafkaSubscriber.getDataframe(sparkSession) match {
      case Some(df) =>
        df.cache()
        DeltaHandler.getDeltaTable(sparkSession, s3DailyBucket) match {
          case Some(table) => DataframeWriter.saveDailyTrends(table, df, s3DailyBucket)
          case None => DataframeWriter.createDailyTrends(df, s3DailyBucket)
        }
      case None => logger.severe("############# Unable to get kafka data")
    }
    sparkSession.close()
  }
}

object TrendsConsumer{
  def main(args: Array[String]): Unit = {
    new TrendsConsumer().startJob()
  }
}