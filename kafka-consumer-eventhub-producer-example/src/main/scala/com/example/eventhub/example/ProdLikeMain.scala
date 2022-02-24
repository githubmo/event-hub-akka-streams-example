package com.example.eventhub.example

import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.stream.Materializer
import com.example.eventhub.example.config.AppConfig
import com.example.eventhub.example.eventhub.EventHubStream
import com.example.eventhub.example.kafka.{ AlpakkaKafka, AlpakkaKafkaImpl }
import com.typesafe.scalalogging.StrictLogging

object ProdLikeMain extends App with StrictLogging {
  val appConfig = AppConfig.load()
  println(appConfig)

  implicit val system = ActorSystem()
  implicit val mat    = Materializer.matFromSystem

  val eventHubStream                    = new EventHubStream(appConfig.eventHub)
  val alpakkaKafkaStreams: AlpakkaKafka = new AlpakkaKafkaImpl
  val kafkaToEventHub = new KafkaToEventHub(alpakkaKafkaStreams, eventHubStream, appConfig.topics.telemetry)

  kafkaToEventHub.singleGraph.run()

  CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, s"kafka_to_event_shutdown") { () =>
    logger.info(s"Draining and shutting down KafkaToEventHub")
    kafkaToEventHub.innerControl.get.shutdown()
  }
}
