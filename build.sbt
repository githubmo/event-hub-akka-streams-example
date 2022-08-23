import CommonForBuild._
import Dependencies._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "Example Org"

lazy val root =
  (project in file("."))
    .settings(name := "Event Hub Examples")
    .settings(commonSettings)
    .aggregate(simpleExample, kafkaExamples)

lazy val simpleExample =
  (project in file("simple-example"))
    .settings(name := "Event Hub Simple Example", libraryDependencies ++= allDeps)
    .settings(commonSettings)

lazy val kafkaExamples =
  (project in file("kafka-consumer-eventhub-producer-example"))
    .settings(name := "Event Hub with Kafka Example", libraryDependencies ++= allDeps :+ scalaLogging)
    .settings(commonSettings)

lazy val javaSimpleExample =
  (project in file("simple-java-example"))
    .settings(
      name := "Java Event Hub Example",
      libraryDependencies ++= allDeps,
      crossPaths       := false,
      autoScalaLibrary := false)
    .settings(commonSettings)
