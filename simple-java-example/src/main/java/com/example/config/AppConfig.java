package com.example.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public final class AppConfig {
    private static final Config appConfig = ConfigFactory.load();
    private static final String connectionString = appConfig.getString("event-hub.connection-string");
    private static final String eventHubName = appConfig.getString("event-hub.event-hub-name");
    private static final String consumerGroup = appConfig.getString("event-hub.consumer-group");

    public static final EventHubConfig eventHubConfig = new EventHubConfig(connectionString, eventHubName, consumerGroup);
}
