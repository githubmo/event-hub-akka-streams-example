package com.example.config;

import java.util.Arrays;
import java.util.Objects;

public record BlobStorageConfig(String connectionString, String container, String sasToken) {

    public BlobStorageConfig {
        Arrays.asList(connectionString, container, sasToken).forEach(s -> {
            Objects.requireNonNull(s);
            if (s.trim().isBlank()) {
                throw new IllegalArgumentException("Configuration for blob storage cannot be empty");
            }
        });
    }
}
