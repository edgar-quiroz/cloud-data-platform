package com.quiroz.CloudDataPlatform.Handlers

import io.delta.tables.DeltaTable
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession, functions}

object DataframeWriter {
  def saveDailyTrends(table: DeltaTable, dataframe: DataFrame, s3Bucket: String): Boolean ={
    table
      .as("history")
      // When events match with existing record
      .merge(
        getDeduplicatedDataFrame(dataframe).as("events"),
        "history.date = events.date and history.keyword = events.keyword"
      )
      .whenMatched
      .updateExpr( Map("search_count" -> "history.search_count + events.search_count") )
      .whenNotMatched
      .insertAll()
      .execute()
    true
  }

  def createDailyTrends(dataframe: DataFrame, s3Bucket: String): Boolean ={
    try{
      getDeduplicatedDataFrame(dataframe)
        .write
        .format("delta")
        .partitionBy("date")
        .mode(SaveMode.Overwrite)
        .save(s3Bucket)
      true
    }catch {
      case _: Exception => false
    }
  }

  def getDeduplicatedDataFrame(df: DataFrame): DataFrame ={
    df
      .select("keyword", "date", "search_count")
      .groupBy("keyword", "date")
      .sum()
      .withColumnRenamed("sum(search_count)", "search_count")
  }
}
