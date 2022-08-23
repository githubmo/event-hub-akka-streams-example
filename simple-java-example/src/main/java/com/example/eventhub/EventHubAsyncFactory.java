package com.example.eventhub;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.example.config.EventHubConfig;

public class EventHubAsyncFactory {
    public final EventHubProducerAsyncClient producer;
    public final EventHubConsumerAsyncClient consumer;

    public EventHubAsyncFactory(EventHubConfig eventHubConfig) {
        var connectionBuilder = new EventHubClientBuilder()
                .connectionString(eventHubConfig.connectionString(), eventHubConfig.eventHubName());

        this.producer = connectionBuilder.buildAsyncProducerClient();

        this.consumer = connectionBuilder
                .consumerGroup(eventHubConfig.consumerGroup())
                .buildAsyncConsumerClient();
    }
}
