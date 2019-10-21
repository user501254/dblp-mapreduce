name := "hw2"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"

libraryDependencies += "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0"
// https://mvnrepository.com/artifact/com.google.code.findbugs/jsr305
libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.2"

// https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-common
libraryDependencies += "org.apache.hadoop" % "hadoop-common" % "3.2.1"
// https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-hdfs
libraryDependencies += "org.apache.hadoop" % "hadoop-hdfs" % "3.2.1"