package com.example.eventhub;

import java.util.Optional;

public record EventHubStreamData(byte[] bytes, Optional<String> partitionKey) {

    private static final int MEGA_BYTE = 1024*1024;
    public EventHubStreamData {
        if (bytes.length <= 0) {
            throw new IllegalArgumentException("bytes sent to Event Hub cannot be empty");
        }

        if (partitionKey.isPresent() && (partitionKey.get() == null || partitionKey.get().isBlank())) {
            throw new IllegalArgumentException("partitionKey cannot contain a null or blank string");
        }

        if (bytes.length >= MEGA_BYTE) {
            throw new IllegalArgumentException("event cannot be bigger than a megabyte");
        }
    }
}
