package com.example.eventhub.example.eventhub

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Source }
import com.azure.messaging.eventhubs.models.CreateBatchOptions
import com.azure.messaging.eventhubs.{ EventData, EventDataBatch, EventHubClientBuilder, EventHubProducerAsyncClient }
import com.example.eventhub.example.config.EventHub as EventHubConfig
import com.example.eventhub.example.eventhub.EventHubStream.*
import com.typesafe.scalalogging.StrictLogging

class EventHubStream(eventHubConfig: EventHubConfig) extends StrictLogging {

  lazy val producer = EventHubStream.createEventHubAsyncProducerClient(eventHubConfig)

  val options = new CreateBatchOptions().setMaximumSizeInBytes(1024 * 1024) // 1MB is 1024*1024 bytes

  // Be aware that we have limitations in maximum size of events and batch
  // https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-quotas
  // Be aware that 1MB is the limit of the batch, not just a single message that gets published to eventhub

  // A flow that publishes to event hub and returns the number of messages published
  val batchFlow: Flow[EventDataBatch, PublishedEventsMetadata, NotUsed] = Flow[EventDataBatch].flatMapConcat { batch =>
    Source
      .fromPublisher(producer.send(batch))
      .map(_ => PublishedEventsMetadata(numberOfEvents = batch.getCount, sizeInBytes = batch.getSizeInBytes))
  }

  // A flow that publishes a single event to eventhub
  val singleEventFlow: Flow[EventData, PublishedEventsMetadata, NotUsed] = Flow[EventData].flatMapConcat { eventData =>
    Source
      .fromPublisher(producer.send(java.util.Collections.singleton(eventData)).map(_ => logger.info("sent an event")))
      .map(_ => PublishedEventsMetadata(numberOfEvents = 1, eventData.getBody.length))
  }
}

object EventHubStream {

  case class PublishedEventsMetadata(numberOfEvents: Int, sizeInBytes: Int)

  private[eventhub] def createEventHubAsyncProducerClient(eventHubConfig: EventHubConfig): EventHubProducerAsyncClient =
    new EventHubClientBuilder()
      .connectionString(eventHubConfig.connectionString, eventHubConfig.eventHubName)
      .buildAsyncProducerClient()
}
