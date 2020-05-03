name := "com.quiroz.CloudDataPlatform.TrendsConsumer"

version := "0.1"

scalaVersion := "2.11.12"

// https://mvnrepository.com/artifact/org.apache.spark/spark-sql
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.5" //% "provided"

libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.4.5" //% "provided"
// https://mvnrepository.com/artifact/org.apache.spark/spark-sql-kafka-0-10
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.5" //% "provided"

// S3 support
// https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-aws
libraryDependencies += "org.apache.hadoop" % "hadoop-aws" % "2.6.5"

// Delta Lake
// https://mvnrepository.com/artifact/io.delta/delta-core
libraryDependencies += "io.delta" %% "delta-core" % "0.6.0" //% "provided"

//mainClass
assemblyOutputPath in assembly := file("./output/artifacts/com.quiroz.CloudDataPlatform.TrendsConsumer.jar")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}