package com.example.eventhub.example

import com.example.eventhub.example.config.AppConfig
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord }
import org.apache.kafka.common.serialization.{ ByteArraySerializer, StringSerializer }

import scala.jdk.CollectionConverters.*
import scala.util.Random

object ProduceTelemetry extends App {
  val configs = Map[String, AnyRef](
    "bootstrap.servers" -> "localhost:9092",
    "client.id"         -> "milton-it-test",
    "acks"              -> "all").asJava
  val kafkaProducer = new KafkaProducer(configs, new StringSerializer, new ByteArraySerializer)

  val appConfig = AppConfig.load()
  val topic     = appConfig.topics.telemetry

  def randomStringBytes = Random.alphanumeric.take(2000).mkString.getBytes()

  (1 until 10000).foreach(_ => kafkaProducer.send(new ProducerRecord[String, Array[Byte]](topic, randomStringBytes)))
}
