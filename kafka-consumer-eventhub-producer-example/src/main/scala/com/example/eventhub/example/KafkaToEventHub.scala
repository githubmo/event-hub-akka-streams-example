package com.example.eventhub.example

import akka.NotUsed
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.{ ConsumerMessage, ProducerMessage }
import akka.stream.scaladsl.Source
import com.example.eventhub.example.KafkaToEventHub.createEventHubSource
import com.example.eventhub.example.eventhub.EventHubStream
import com.example.eventhub.example.kafka.AlpakkaKafka

import java.util.concurrent.atomic.AtomicReference

class KafkaToEventHub(alpakkaKafka: AlpakkaKafka, eventHubStream: EventHubStream, inputTopic: String) {

  val innerControl: AtomicReference[Consumer.Control] = new AtomicReference[Control](Consumer.NoopControl)

  val singleGraph = alpakkaKafka
    .byteArrayConsumer(inputTopic)
    .mapMaterializedValue(c => innerControl.set(c))
    .flatMapConcat { msg =>
      createEventHubSource(eventHubStream, msg.record.value, msg.committableOffset)
    }
    .to(alpakkaKafka.byteArrayProducer)

  def stop = innerControl.get().shutdown()
}

object KafkaToEventHub {
  def createEventHubSource(
      eventHub: EventHubStream,
      bytes: Array[Byte],
      committableOffset: ConsumerMessage.CommittableOffset)
      : Source[ProducerMessage.Envelope[String, Array[Byte], ConsumerMessage.CommittableOffset], NotUsed] = {
    eventHub
      .singleEventSource(bytes)
      .map(_ => ProducerMessage.passThrough[String, Array[Byte], ConsumerMessage.CommittableOffset](committableOffset))
  }
}
