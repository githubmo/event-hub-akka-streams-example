package com.example.eventhub.example.kafka

import akka.kafka.*
import akka.kafka.ConsumerMessage.*
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.scaladsl.{ Committer, Consumer, Producer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.{ Done, NotUsed }
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.{
  ByteArrayDeserializer,
  ByteArraySerializer,
  StringDeserializer,
  StringSerializer
}

import scala.concurrent.Future

trait AlpakkaKafka {
  def byteArrayProducer: Sink[ProducerMessage.Envelope[String, Array[Byte], Committable], Future[Done]]
  def byteArrayConsumer(topics: String*): Source[CommittableMessage[String, Array[Byte]], Control]
  def committerFlow: Flow[Committable, Done, NotUsed]
}

class AlpakkaKafkaImpl extends AlpakkaKafka {

  // alpakka kafka knows how to extract configuration hence why
  // we don't use the `AppConfig`
  // We're doing it this way instead of using the `ActorSystem` method
  // so we can reduce complexity in our code
  private val config = ConfigFactory.load()

  private val producerConfig    = config.getConfig(ProducerSettings.configPath)
  private val consumerConfig    = config.getConfig(ConsumerSettings.configPath)
  private val committerSettings = CommitterSettings(config.getConfig(CommitterSettings.configPath))

  def byteArrayProducerSettings: ProducerSettings[String, Array[Byte]] =
    ProducerSettings(producerConfig, new StringSerializer, new ByteArraySerializer)

  def byteArrayConsumerSettings: ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)

  override def byteArrayProducer: Sink[ProducerMessage.Envelope[String, Array[Byte], Committable], Future[Done]] =
    Producer.committableSink(byteArrayProducerSettings, committerSettings)

  override def byteArrayConsumer(topics: String*): Source[CommittableMessage[String, Array[Byte]], Control] =
    Consumer.committableSource(byteArrayConsumerSettings, Subscriptions.topics(topics*))

  override def committerFlow: Flow[Committable, Done, NotUsed] = Committer.flow(committerSettings)
}
