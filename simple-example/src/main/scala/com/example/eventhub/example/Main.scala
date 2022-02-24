package com.example.eventhub.example

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import com.azure.messaging.eventhubs.EventData
import com.example.eventhub.example.config.AppConfig
import com.example.eventhub.example.eventhub.EventHubStream
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

object Main extends App with StrictLogging {
  logger.info("Starting App")
  val appConfig = AppConfig.load()
  logger.info(appConfig.toString)

  implicit val system = ActorSystem()
  implicit val mat    = Materializer.matFromSystem

  def randomString   = Random.alphanumeric.take(10).mkString
  val eventHubStream = new EventHubStream(appConfig.eventHub)

  val numberOfBytesSentF = Source(1 to 10)
    .map(_ => new EventData(randomString.getBytes))
    .via(eventHubStream.singleEventFlow)
    .runWith(Sink.last)
    .map(_ => logger.info("sent stuff"))

  val numberOfBatchBytesSentF =
    Source(1 to 10)
      .map(_ => randomString.getBytes)
      .grouped(5)
      .flatMapConcat(bytesSeq => eventHubStream.createBatch(bytesSeq))
      .via(eventHubStream.batchFlow)
      .runWith(Sink.last)
      .map(_ => logger.info("sent batch stuff"))
      .map(_ => logger.info("sent batch stuff"))
}
