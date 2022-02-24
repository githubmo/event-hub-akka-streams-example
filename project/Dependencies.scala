import sbt._

object Dependencies {
  // Azure Eventhub
  val azureMessagingEventHub = "com.azure" % "azure-messaging-eventhubs" % "5.11.0"

  // Akka Stream and Alpakka Kafka
  val akkaStream      = "com.typesafe.akka" %% "akka-stream"       % "2.6.18"
  val akkaStreamKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0"

  // Configuration
  val typesafeConfig = "com.typesafe"           % "config"     % "1.4.2"
  val pureConfig     = "com.github.pureconfig" %% "pureconfig" % "0.17.1"
  // logging
  val logbackClassic = "ch.qos.logback"              % "logback-classic" % "1.2.10"
  val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.4"

  // SBT test
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.9"

  val allDeps = Seq(
    azureMessagingEventHub,
    akkaStream,
    akkaStreamKafka,
    typesafeConfig,
    pureConfig,
    logbackClassic,
    scalaLogging,
    scalaTest)
}
