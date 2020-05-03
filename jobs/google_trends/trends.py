import pandas as pd                        
from pytrends.request import TrendReq
from datetime import date
#Spark
from pyspark.sql import *

# https://github.com/GeneralMills/pytrends

spark = SparkSession.builder.master("local").appName("trends").getOrCreate()
sparkConf = spark.sparkContext._conf
sparkConf.set("spark.io.compression.codec", "snappy")

# Trending hotel searches from mexico city

# https://github.com/GeneralMills/pytrends
pytrends = TrendReq()
# Search keywords
keyword = 'cancun'
# https://github.com/pat310/google-trends-api/wiki/Google-Trends-Categories
category = 179
# Get only last day
timeframe = 'now 1-d'

# Limit searches from mexico city
# 'MX' works for all regions of Mexico
geo = 'MX-DIF'

pytrends.build_payload(kw_list=[keyword], geo=geo, timeframe=timeframe, cat=category)
# Get related queries of keywords
result = pytrends.related_queries()
# Get a single dataframe by joining both search types
df = result[keyword]['top']
# trim keyword, category name and remove whitespaces
df['keyword'] = df['query'].apply(lambda x: x.replace(keyword, '').replace('hotel', '').strip())

# Adding time columns for partitioning and aggregation
today = date.today()
df['date'] = today.strftime("%Y-%m-%d")
df['month'] = today.strftime("%m")
df['search_count'] = df['value'].astype('int64')

# Convert dataframe to spark
sparkDF = spark.createDataFrame(df)

# Write result to kafka
sparkDF \
	.select('keyword', 'search_count', 'date', 'month') \
	.selectExpr("CAST(date AS STRING) AS key", "to_json(struct(*)) AS value") \
  .write \
  .format("kafka") \
  .option("kafka.bootstrap.servers", "kafka:9092") \
  .option("topic", "trends") \
  .option("key.serializer", "org.apache.kafka.common.serialization.StringSerializer") \
  .option("value.serializer", "org.apache.kafka.common.serialization.StringSerializer") \
  .save()
