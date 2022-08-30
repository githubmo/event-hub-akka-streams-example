package com.example.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public final class AppConfig {

    // Eventhub configuration
    private static final Config appConfig = ConfigFactory.load();
    private static final String eventHubConnectionString = appConfig.getString("event-hub.connection-string");
    private static final String eventHubName = appConfig.getString("event-hub.event-hub-name");
    private static final String consumerGroup = appConfig.getString("event-hub.consumer-group");

    public static final EventHubConfig eventHubConfig = new EventHubConfig(eventHubConnectionString, eventHubName, consumerGroup);

    // Blob storage configuration
    private static final String blobConnectionString = appConfig.getString("blob-storage.connection-string");

    private static final String blobContainerName = appConfig.getString("blob-storage.container-name");

    private static final String blobSasToken = appConfig.getString("blob-storage.sas-token");

    public static final BlobStorageConfig blobStorageConfig = new BlobStorageConfig(blobConnectionString, blobContainerName, blobSasToken);
}
