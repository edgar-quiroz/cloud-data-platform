# Cloud Data Platform  
  
This proyect tends to be a cloud data plarform approach, it will be updated in several releases incorporating more big data technologies, this first version only supports spark batch processing, kafka source and AWS S3 as storage layer.  
  
## Components  
  
* **Zookeeper**.- Three Zookeeper instances for cluster coordination zoo1:2181,zoo2:2181,zoo3:2181 using official [zookeeper](https://hub.docker.com/_/zookeeper).  
* **Kafka**.- One kafka server/broker using a custom image, [git](https://github.com/edgar-quiroz/kafka), [edgarquiroz/kafka](https://hub.docker.com/repository/docker/edgarquiroz/kafka).  
* **Spark**.- Spark jobs docker base image, [git](https://github.com/edgar-quiroz/spark), [edgarquiroz/spark](https://hub.docker.com/repository/docker/edgarquiroz/spark).  
* **Google trends**.- pyspark application that collects google trends by using pytrends library and publish results to kafka.  
* **Trends consumer**.- Scala spark applications that connects to kafka, consumes trends published, applies summarization and store result using Delta Lake in S3.  
  
## Google trends pyspark job  
This job collects Google trends searches using pytrends library.  
> pytrends = TrendReq()  
  
Sets the location search scope, MX means "All Mexico", MX-DIF means "Mexico, Mexico City", so we want all searches made from Mexico City.  
> geo = 'MX-DIF'  
  
Than contains 'cancun'.  
> keyword = 'cancun'  
  
For [category](https://github.com/pat310/google-trends-api/wiki/Google-Trends-Categories) 179.
If not category set it will look for all categories (all searches).  
> category = 179  
  
And only last day searches (yesterday).   
> timeframe = 'now 1-d'  
  
Get trends.  
> pytrends.build_payload(kw_list=[keyword], geo=geo, timeframe=timeframe, cat=category)
> result = pytrends.related_queries()
  
'top' contains a pandas dataframe, convert this dataframe to spark dataframe and publish to kafka.  
Be sure that **kafka.bootstrap.servers** points to a valid kafka hostname.  
> .option("kafka.bootstrap.servers", "kafka:9092")
  
Next step is consume all data published, perform some processing and then persist it at AWS S3.  
  
## Trends consumer spark job
This jobs connects to kafka and receives all data published, one of the important thing about this job is that uses **Delta Lake** storage layer library, delta lake allow us to handle data changing and update an existing delta table.  
  
It means that instead of appending and duplicating data, we will update existing records.
  
```scala
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
```
  
If some record already exist search_count will be existing search_count plus new record search_count.  
> .whenMatched.updateExpr( Map("search_count" -> "history.search_count + events.search_count") )  
  
If not exist, new record will be inserted.  
> .whenNotMatched.insertAll()
  
## Instructions  
  
cd-to docker-compose.yaml folder and execute **docker-compose up**.  
Once all services are running in other terminal execute **docker-compose ps** to find docker containers name, in my case that names are:  
  
* cloud_data_platform_kafka_1  
* cloud_data_platform_trends_1  
* cloud_data_platform_trends_consumer_1  
* cloud_data_platform_zoo1_1  
* cloud_data_platform_zoo2_1  
* cloud_data_platform_zoo3_1  
  
Connect to kafka container using:  
> docker exec -it cloud_data_platform_kafka_1 bash 
  
Create kafka topic.  
> /etc/kafka/bin/kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 1 --partitions 1 --topic trends  
  
List topics (trends topic must be displayed).  
> /etc/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092  
    
Connect to google_trends instance and execute following command to produce kafka messages.  
> docker exec -it cloud_data_platform_trends_1 bash  
> /etc/spark/bin/spark-submit --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.0.0-preview2 trends.py  
  
Download trends_consumer, update AWS S3 with your bucket and credentials, navigate to root path and generate artifact with:  
> sbt assembly  
  
Once jar file generated (be sure path match with docker compose mount volume).  
  
Connect to trends_consumer instance and execute following command:  
> spark-submit --master=local[*] --packages io.delta:delta-core_2.11:0.6.0,org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.5,org.apache.spark:spark-streaming_2.11:2.4.5 --class com.quiroz.CloudDataPlatform.TrendsConsumer /work/artifacts/com.quiroz.CloudDataPlatform.TrendsConsumer.jar 
  
### Google trends produced messages
```json
{"keyword":"fiesta americana","search_count":100,"date":"2020-05-03","month":"05"}
{"keyword":"riu","search_count":71,"date":"2020-05-03","month":"05"}
{"keyword":"moon palace","search_count":64,"date":"2020-05-03","month":"05"}
{"keyword":"barcelo","search_count":59,"date":"2020-05-03","month":"05"}
{"keyword":"iberostar","search_count":44,"date":"2020-05-03","month":"05"}
{"keyword":"barcelo","search_count":28,"date":"2020-05-03","month":"05"}
{"keyword":"casa maya","search_count":25,"date":"2020-05-03","month":"05"}
{"keyword":"grand fiesta americana coral beach","search_count":24,"date":"2020-05-03","month":"05"}
{"keyword":"fiesta americana villas","search_count":23,"date":"2020-05-03","month":"05"}
{"keyword":"presidente intercontinental","search_count":17,"date":"2020-05-03","month":"05"}
{"keyword":"best day","search_count":14,"date":"2020-05-03","month":"05"}
{"keyword":"aquamarina beach","search_count":12,"date":"2020-05-03","month":"05"}
{"keyword":"casa maya","search_count":11,"date":"2020-05-03","month":"05"}
{"keyword":"fiesta americana condesa","search_count":11,"date":"2020-05-03","month":"05"}
{"keyword":"holiday inn  arenas","search_count":10,"date":"2020-05-03","month":"05"}
```  
  
After executing twice trends consumer i get:  
  
|keyword|date|search_count|
|---|---|---|
|presidente intercontinental|2020-05-03|34|
|fiesta americana condesa|2020-05-03|22|
|fiesta americana villas|2020-05-03|46|
|holiday inn  arenas|2020-05-03|20|
|aquamarina beach|2020-05-03|24|
|fiesta americana|2020-05-03|200|
|grand fiesta americana coral beach|2020-05-03|48|
|moon palace|2020-05-03|128|
|casa maya|2020-05-03|72|
|iberostar|2020-05-03|88|
|best day|2020-05-03|28|
|barcelo|2020-05-03|174|
|riu|2020-05-03|142| 

