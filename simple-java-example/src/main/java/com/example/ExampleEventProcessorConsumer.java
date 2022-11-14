package com.example;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.azure.messaging.eventhubs.models.EventContext;
import com.example.config.AppConfig;
import com.example.eventhub.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class ExampleEventProcessorConsumer {
    private static final Logger logger = LoggerFactory.getLogger("ExampleEventProcessorConsumer");

    public static void main(String[] args) {
        var eventHubAsyncFactory = new EventHubAsyncFactory(AppConfig.eventHubConfig);
        var eventHubStreamProducer = new EventHubStreamProducer(eventHubAsyncFactory.producer);

        var system = ActorSystem.create();

        var sink = Flow.of(EventOrErrorContext.class)
                .throttle(1, Duration.ofSeconds(3))
                .map( e -> {
                    if (!e.isError()) {
                        var message = e.getContext().getEventData().getBodyAsString();
                        logger.info("Recieved the following message: {}", message);
                        return message;
                    } else {
                        var message = "Error received with the following exception: " + e.getErrorContext().getThrowable().getMessage();
                        logger.error(message);
                        return message;
                    }
                }).toMat(Sink.ignore(), Keep.right());

        var eventProcessorConsumerSource = EventProcessorSource.sourceFrom(
                AppConfig.eventHubConfig,
                AppConfig.blobStorageConfig,
                10
        );

        eventProcessorConsumerSource.to(sink).run(system);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                system.terminate();
                try {
                    system.getWhenTerminated().toCompletableFuture().get(20, TimeUnit.SECONDS);
                } catch(Exception e) {
                    logger.error("Could not complete system shutdown", e);
                }
            }
        }));
//        system.scheduler().scheduleOnce(Duration.ofSeconds(30), eventProcessorConsumer::stop, system.dispatcher());
//        system.terminate();
//        system.getWhenTerminated().toCompletableFuture().join();
//        System.exit(0);
    }
}
