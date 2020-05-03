package com.quiroz.CloudDataPlatform.SparkSession

import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object SparkBuilder {
  // Use this function when you already know the right job behavoir
  // See defaults here https://spark.apache.org/docs/latest/configuration.html
  // spark.speculation
  def getSession(appName: String): SparkSession ={
    SparkSession
      .builder()
      .appName(appName)
      // UI block
      .config("spark.ui.retainedJobs", "1")
      .config("spark.ui.retainedStages", "1")
      .config("spark.ui.retainedTasks", "100")
      .config("spark.worker.ui.retainedExecutors", "1")
      .config("spark.worker.ui.retainedDrivers", "1")
      .config("spark.sql.ui.retainedExecutions", "1")
      .config("spark.streaming.ui.retainedBatches", "10")
      .config("spark.ui.retainedDeadExecutors", "5")
      // Compression
      .config("spark.io.compression.codec", "org.apache.spark.io.SnappyCompressionCodec")
      // Parallelism
      .config("spark.default.parallelism", "100")
      .config("spark.sql.shuffle.partitions", "100")
      // Cloud algorithm version, S3 support and Delta lake
      .config("spark.sql.parquet.filterPushdown", "true")
      .config("spark.sql.parquet.mergeSchema", "false")
      .config("spark.hadoop.mapreduce.fileoutputcommitter.algorithm.version", "2")
      .config("mapreduce.outputcommitter.factory.scheme.s3a", "org.apache.hadoop.fs.s3a.commit.S3ACommitterFactory")
      .config("fs.s3a.committer.name", "partitioned")
      .config("fs.s3a.committer.staging.conflict-mode", "append")
      //.config("spark.hadoop.fs.s3a.multiobjectdelete.enable","false")
      .config("spark.hadoop.fs.s3a.fast.upload","true")
      .config("spark.delta.logStore.class", "org.apache.spark.sql.delta.storage.S3SingleDriverLogStore")

      .config("spark.speculation", "false")
      // Streaming
      .config("spark.streaming.stopGracefullyOnShutdown", "true")
      .config("spark.streaming.ui.retainedBatches", "10")
      .getOrCreate()
  }
}
