package com.example;

import akka.actor.ActorSystem;
import akka.stream.KillSwitches;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import com.example.config.AppConfig;
import com.example.eventhub.*;
import com.example.eventhub.eventprocessor.single.EventOrErrorContext;
import com.example.eventhub.eventprocessor.single.EventProcessorSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ExampleEventProcessorConsumer {
    private static final Logger logger = LoggerFactory.getLogger("ExampleEventProcessorConsumer");

    public static void main(String[] args) {
        var eventHubAsyncFactory = new EventHubAsyncFactory(AppConfig.eventHubConfig);
        var eventHubStreamProducer = new EventHubStreamProducer(eventHubAsyncFactory.producer);

        var system = ActorSystem.create();

//        var sink = Flow.of(EventOrErrorContext.class)
//                .throttle(1, Duration.ofSeconds(3))
//                .mapConcat( e -> {
//                    if (!e.isError()) {
//                        var message = e.getContext().getEventData().getBodyAsString();
//                        logger.info("Recieved the following message: {}", message);
//                        return Collections.singleton(e.getContext());
//                    } else {
//                        var message = "Error received with the following exception: " + e.getErrorContext().getThrowable().getMessage();
//                        logger.error(message);
//                        return Collections.emptySet();
//                    }
//                })
//                .map(e -> {
//                    e.updateCheckpoint();
//                    return true;
//                })
//                .toMat(Sink.ignore(), Keep.right());

        var sink = Flow.of(EventOrErrorContext.class)
//                .throttle(1, Duration.ofSeconds(3))
                .mapConcat(e -> {
                    if (!e.isError()) {
                        var message = e.getContext().getEventData().getBodyAsString();
                        logger.info("got something");
                        return Collections.singleton(e.getContext());
                    } else {
                        logger.info("got error");
                        return Collections.emptySet();
                    }
                })
                .takeWithin(java.time.Duration.ofSeconds(10))
                .fold(0, (count, e) -> {
                    return count + 1;
                })
                .map(count -> {
                    logger.info("Retrieved {} events in 10 seconds", count);
                    return count;
                })
                .toMat(Sink.ignore(), Keep.right());

        var eventProcessorConsumerSource = EventProcessorSource.sourceFrom(
                AppConfig.eventHubConfig,
                AppConfig.blobStorageConfig,
                20
        );

        var killSwitches = KillSwitches.shared("example-kill-switch");

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                system.terminate();
                try {
                    killSwitches.abort(new RuntimeException("Program has been suspended"));
                    system.getWhenTerminated().toCompletableFuture().get(20, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.error("Could not complete system shutdown", e);
                }
            }
        }));

        eventProcessorConsumerSource.via(killSwitches.flow()).to(sink).run(system);

    }
}
