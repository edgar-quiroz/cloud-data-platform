package com.quiroz.CloudDataPlatform.Handlers

import io.delta.tables.DeltaTable
import org.apache.spark.sql.SparkSession

object DeltaHandler {
  def getDeltaTable(sparkSession: SparkSession, s3Bucket: String): Option[DeltaTable] ={
    try{
      Some(DeltaTable.forPath(sparkSession, s3Bucket))
    }catch {
      case _: Exception => None
    }
  }
}
