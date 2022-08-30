package com.example;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.azure.messaging.eventhubs.models.EventContext;
import com.example.config.AppConfig;
import com.example.eventhub.EventHubAsyncFactory;
import com.example.eventhub.EventHubStreamConsumer;
import com.example.eventhub.EventHubStreamProducer;
import com.example.eventhub.EventProcessorConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ExampleEventProcessorConsumer {
    private static final Logger logger = LoggerFactory.getLogger("ExampleEventProcessorConsumer");

    public static void main(String[] args) {
        var eventHubAsyncFactory = new EventHubAsyncFactory(AppConfig.eventHubConfig);
        var eventHubStreamProducer = new EventHubStreamProducer(eventHubAsyncFactory.producer);
        var eventHubStreamConsumer = new EventHubStreamConsumer(eventHubAsyncFactory.consumer);

        var system = ActorSystem.create();

        var sink = Flow.of(EventContext.class)
                .throttle(1, Duration.ofSeconds(3))
                .flatMapConcat(e ->
                    Source.single(e.getEventData().getBodyAsString())
                            .map(message -> {
                                System.out.println(message);
                                return message;
                            })
                            .map(message -> {
                                logger.info("Committing a message");
                                e.updateCheckpoint();
                                return message;
                            })
                ).toMat(Sink.ignore(), Keep.right());

        var eventProcessorConsumer = new EventProcessorConsumer(
               AppConfig.eventHubConfig,
               AppConfig.blobStorageConfig,
               sink,
               system
        );

        eventProcessorConsumer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(eventProcessorConsumer::stop));
//        system.scheduler().scheduleOnce(Duration.ofSeconds(30), eventProcessorConsumer::stop, system.dispatcher());
//        system.terminate();
//        system.getWhenTerminated().toCompletableFuture().join();
//        System.exit(0);
    }
}
