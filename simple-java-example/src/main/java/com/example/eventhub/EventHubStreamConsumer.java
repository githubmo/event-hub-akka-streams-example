package com.example.eventhub;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class EventHubStreamConsumer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final EventHubConsumerAsyncClient consumerAsyncClient;

    public EventHubStreamConsumer(EventHubConsumerAsyncClient consumerAsyncClient) {
        this.consumerAsyncClient = consumerAsyncClient;
    }

    // Creating as a method for lazy creation
    public Source<EventHubStreamData, NotUsed> eventHubEvents() {
        return Source
                .fromPublisher(consumerAsyncClient.receive())
                .map(partitionEvent ->
                        new EventHubStreamData(partitionEvent.getData().getBody(), Optional.of(partitionEvent.getPartitionContext().getPartitionId())));
    }
}
