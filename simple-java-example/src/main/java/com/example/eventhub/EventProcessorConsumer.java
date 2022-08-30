package com.example.eventhub;

import akka.stream.javadsl.Source;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.example.config.BlobStorageConfig;
import com.example.config.EventHubConfig;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProcessorConsumer {

   private final Logger logger = LoggerFactory.getLogger(this.getClass());


   private BlobContainerAsyncClient blobContainerAsyncClient;

   private EventHubConfig eventHubConfig;

   private EventProcessorClient eventProcessorClient;


   private EventProcessorConsumer(EventHubConfig eventHubConfig, BlobStorageConfig blobStorageConfig) {
      this.blobContainerAsyncClient = new BlobContainerClientBuilder()
              .connectionString(blobStorageConfig.connectionString())
              .containerName(blobStorageConfig.container())
              .sasToken(blobStorageConfig.sasToken())
              .buildAsyncClient();
      this.eventHubConfig = eventHubConfig;
      this.eventProcessorClient =
              new EventProcessorClientBuilder()
                      .connectionString(eventHubConfig.connectionString())
                      .consumerGroup(eventHubConfig.consumerGroup())
                      .processEvent()
                      .processPartitionClose()
                      .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
                      .buildEventProcessorClient();

      Source.actorRef()
   }


   private class StreamFeeder extends AbstractBehavior

}
