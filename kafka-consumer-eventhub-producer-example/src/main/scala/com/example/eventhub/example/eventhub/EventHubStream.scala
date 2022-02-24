package com.example.eventhub.example.eventhub

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Source }
import com.azure.messaging.eventhubs.models.CreateBatchOptions
import com.azure.messaging.eventhubs.{ EventData, EventDataBatch, EventHubClientBuilder, EventHubProducerAsyncClient }
import com.example.eventhub.example.config.EventHub as EventHubConfig
import com.example.eventhub.example.eventhub.EventHubStream.*
import com.typesafe.scalalogging.StrictLogging
import reactor.core.publisher.Mono

class EventHubStream(eventHubConfig: EventHubConfig) extends StrictLogging {

  lazy val producer = EventHubStream.createEventHubAsyncProducerClient(eventHubConfig)

  val options = new CreateBatchOptions().setMaximumSizeInBytes(1024 * 1024) // 1MB is 1024*1024 bytes

  // Be aware that we have limitations in maximum size of events and batch
  // https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-quotas
  // Be aware that 1MB is the limit of the batch, not just a single message that gets published to eventhub

  def createBatch(bytesSeq: Seq[Array[Byte]]): Source[EventDataBatch, NotUsed] = {
    Source
      .fromPublisher {
        logger.info(s"Creating a batch from ${bytesSeq.length} elements")
        producer.createBatch(options)
      }
      .map { batch =>
        bytesSeq.foreach(bytes => batch.tryAdd(new EventData(bytes)))
        batch
      }
  }

  // A flow that publishes to event hub and returns the number of messages published
  val batchFlow: Flow[EventDataBatch, PublishedEventsMetadata, NotUsed] = Flow[EventDataBatch].flatMapConcat { batch =>
    Source
      .fromPublisher {
        logger.info(s"Attempting to send ${batch.getCount} events to Event Hub")
        producer.send(batch).doOnSuccess(_ => logger.info("batch event sent")).`then`(Mono.just(true))
      }
      .map(_ => PublishedEventsMetadata(numberOfEvents = batch.getCount, sizeInBytes = batch.getSizeInBytes))
  }

  // A flow that publishes a single event to eventhub
  val singleEventFlow: Flow[EventData, PublishedEventsMetadata, NotUsed] = Flow[EventData].flatMapConcat { eventData =>
    Source
      .fromPublisher {
        logger.info("Attempting to send a single event")
        producer
          .send(java.util.Collections.singleton(eventData))
          .doOnSuccess(_ => logger.info("single event sent"))
          .`then`(Mono.just(true))
      }
      .map(_ => PublishedEventsMetadata(numberOfEvents = 1, eventData.getBody.length))
  }

  def singleEventSource(bytes: Array[Byte]) = {
    Source.single(new EventData(bytes)).via(singleEventFlow)
  }
}

object EventHubStream {

  case class PublishedEventsMetadata(numberOfEvents: Int, sizeInBytes: Int) {
    def add(other: PublishedEventsMetadata): PublishedEventsMetadata =
      PublishedEventsMetadata(numberOfEvents + other.numberOfEvents, sizeInBytes + other.sizeInBytes)
  }

  private[eventhub] def createEventHubAsyncProducerClient(eventHubConfig: EventHubConfig): EventHubProducerAsyncClient =
    new EventHubClientBuilder()
      .connectionString(eventHubConfig.connectionString, eventHubConfig.eventHubName)
      .buildAsyncProducerClient()
}
