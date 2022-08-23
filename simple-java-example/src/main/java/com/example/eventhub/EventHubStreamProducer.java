package com.example.eventhub;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.azure.messaging.eventhubs.models.CreateBatchOptions;
import com.azure.messaging.eventhubs.models.SendOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class EventHubStreamProducer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private EventHubProducerAsyncClient producerAsyncClient;

    private final int MEGA_BYTE = 1024*1024;

    private final CreateBatchOptions batchOptions = new CreateBatchOptions().setMaximumSizeInBytes(MEGA_BYTE);// 1MB is 1024*1024 bytes

    // Be aware that we have limitations in maximum size of events and batch
    // https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-quotas
    // Be aware that 1MB is the limit of the batch, not just a single message that gets published to eventhub

    public EventHubStreamProducer(EventHubProducerAsyncClient producerAsyncClient) {
        this.producerAsyncClient = producerAsyncClient;
    }

    private final Flow<EventHubStreamData, EventHubStreamData, NotUsed> singleDataFlow = Flow.of(EventHubStreamData.class);

    public final Sink<EventHubStreamData, CompletionStage<Done>> singleEventSink =
            singleDataFlow.flatMapConcat(data -> {
                logger.info("Attempting to send a single event");
                var sendOptions = new SendOptions();
                if (data.partitionKey().isPresent()) {
                    sendOptions.setPartitionKey(data.partitionKey().get());
                }
                return Source.fromPublisher(producerAsyncClient
                    .send(Collections.singleton(new EventData(data.bytes())), sendOptions)
                    .doOnSuccess(unused -> logger.info("single event sent"))
                    .then(Mono.just(true))
                ); // this is to stop the fire and forget nature of `Mono<Void>`
            }).toMat(Sink.ignore(), Keep.right());

    // Sending batched events with keys
    // if you send an `Iterable<EventHub>` to `producerAsyncClient.send`, it will use a batch event to send the events
    public final Sink<EventHubStreamData, CompletionStage<Done>> batchEventSink =
        Flow.of(EventHubStreamData.class)
            .groupedWeightedWithin((long) MEGA_BYTE, e -> (long) e.bytes().length, Duration.ofSeconds(5))
            .mapConcat(eList -> {
                return eList.stream()
                        .collect(Collectors.groupingBy(e -> e.partitionKey().orElse("")))
                        .entrySet();
            })
            .flatMapConcat(entrySet -> {
                var events = entrySet.getValue().stream().map(e -> new EventData(e.bytes())).toList();
                var sendOption = new SendOptions();
                if (!entrySet.getKey().isBlank()) {
                    sendOption.setPartitionKey(entrySet.getKey());
                }
                return Source
                        .fromPublisher(
                                producerAsyncClient.send(events, sendOption)
                                        .doOnSuccess(unused -> logger.info("batch event sent"))
                                        .then(Mono.just(true)));
            }).toMat(Sink.ignore(), Keep.right());
}
