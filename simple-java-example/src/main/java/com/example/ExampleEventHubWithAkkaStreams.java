package com.example;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.example.config.AppConfig;
import com.example.eventhub.EventHubAsyncFactory;
import com.example.eventhub.EventHubStreamConsumer;
import com.example.eventhub.EventHubStreamData;
import com.example.eventhub.EventHubStreamProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public class ExampleEventHubWithAkkaStreams {

    private static Logger logger = LoggerFactory.getLogger("ExampleEventHubWithAkkaStreams");

    public static void main(String[] args) {
        var eventHubAsyncFactory = new EventHubAsyncFactory(AppConfig.eventHubConfig);
        var eventHubStreamProducer = new EventHubStreamProducer(eventHubAsyncFactory.producer);
        var eventHubStreamConsumer = new EventHubStreamConsumer(eventHubAsyncFactory.consumer);

        var system = ActorSystem.create();

//        var completionStage = Source
//                .from(Stream.generate(() -> randomString()).limit(10).toList())
//                .map(s -> new EventHubStreamData(s.getBytes(StandardCharsets.UTF_8), Optional.of(s.substring(0, 1))))
//                .toMat(eventHubStreamProducer.singleEventSink, Keep.right())
//                .run(system);

        var producerCompletionStage = Source
                .from(Stream.generate(ExampleEventHubWithAkkaStreams::randomString).limit(10).toList())
                .map(s -> new EventHubStreamData(s.getBytes(StandardCharsets.UTF_8), Optional.of(s.substring(0, 1))))
                .toMat(eventHubStreamProducer.batchEventSink, Keep.right())
                .run(system);

        var consumerCompletionStage = eventHubStreamConsumer.eventHubEvents()
                        .toMat(Sink.foreach(e -> {
                            var value = new String(e.bytes(), StandardCharsets.UTF_8);
                            var key = e.partitionKey().orElse("");
                            // eventhub hashes the key we give it into a partition number
                            // so here the key printed would be a string representation of the partition number
                            logger.info("Received {} from Event Hub with key {}", value, key);
                        }), Keep.right())
                        .run(system);

        producerCompletionStage.toCompletableFuture().join(); // asynchronous wait
        system.terminate();
        system.getWhenTerminated().toCompletableFuture().join();
    }

    private static final Random random = new Random();

    // a silly copy pasta way of generating random strings
    private static String randomString() {
        var leftLimit = 97; // letter 'a'
        var rightLimit = 122; // letter 'z'
        var targetStringLength = 10;
        return random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
