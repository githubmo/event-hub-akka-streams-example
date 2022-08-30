package com.example.eventhub;

import akka.Done;
import akka.actor.ActorRef;
import static akka.pattern.Patterns.ask;
import akka.actor.ClassicActorSystemProvider;
import akka.stream.CompletionStrategy;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.example.config.BlobStorageConfig;
import com.example.config.EventHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class EventProcessorConsumer {

   private final Logger logger = LoggerFactory.getLogger(this.getClass());


   private BlobContainerAsyncClient blobContainerAsyncClient;

   private EventHubConfig eventHubConfig;

   private EventProcessorClient eventProcessorClient;

   private Sink<EventContext, CompletionStage<Done>> sink;

   private ClassicActorSystemProvider system;

   private ActorRef actorRef;

   public EventProcessorConsumer(EventHubConfig eventHubConfig,
                                  BlobStorageConfig blobStorageConfig,
                                  Sink<EventContext, CompletionStage<Done>> sink,
                                  ClassicActorSystemProvider system
                                  ) {

      this.blobContainerAsyncClient = new BlobContainerClientBuilder()
              .endpoint(blobStorageConfig.connectionString())
              .containerName(blobStorageConfig.container())
              .sasToken(blobStorageConfig.sasToken())
              .buildAsyncClient();
      this.eventHubConfig = eventHubConfig;
      this.sink = sink;
      this.system = system;
   }


   private Source<EventContext, ActorRef> source =
           Source.<EventContext>actorRefWithBackpressure(
                   "ack",
                   // complete when we send "complete"
                   o -> {
                      if (o == "complete") return Optional.of(CompletionStrategy.draining());
                      else return Optional.empty();
                   },
                   // do not fail on any message
                   o -> Optional.empty());

   public void start() {
      this.actorRef = source.to(sink).run(system);
      logger.info("Starting the EventProcessorClient");
      this.eventProcessorClient = new EventProcessorClientBuilder()
              .connectionString(eventHubConfig.connectionString())
              .eventHubName(eventHubConfig.eventHubName())
              .consumerGroup(eventHubConfig.consumerGroup())
              .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
              .processEvent(eventContext -> {
                  logger.info("attempting to send an event context");
                  ask(this.actorRef, eventContext, Duration.ofSeconds(10)).toCompletableFuture().join();
                  logger.info("Sent an event context");
              })
              .processError(errorContext -> {
                  logger.error("EventProcessorClient had a hiccup", errorContext.getThrowable());
              })
              .buildEventProcessorClient();
      eventProcessorClient.start();
   }

   public void stop() {
      logger.info("Stopping the EventProcessorClient and stream");
      this.eventProcessorClient.stop();
      this.actorRef.tell("complete", ActorRef.noSender());
   }

}
