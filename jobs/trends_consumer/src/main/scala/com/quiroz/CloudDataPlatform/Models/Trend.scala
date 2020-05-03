package com.quiroz.CloudDataPlatform.Models

import org.apache.spark.sql.Encoders
import org.apache.spark.sql.types.StructType

case class Trend(keyword: String, search_count: Int, date: String, month: String)

object Trend{
  val schema: StructType = Encoders.product[Trend].schema
}