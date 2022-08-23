package com.example.config;

import java.util.Arrays;
import java.util.Objects;

public record EventHubConfig(String connectionString, String eventHubName, String consumerGroup) {
    public EventHubConfig {
        Arrays.asList(connectionString, eventHubName, consumerGroup).forEach(s -> {
            Objects.requireNonNull(s);
            if (s.trim().isBlank()) {
                throw new IllegalArgumentException("Configuration for eventhub cannot be empty");
            }
        });
    }
}