package com.example.eventhub.eventprocessor.single;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.example.config.BlobStorageConfig;
import com.example.config.EventHubConfig;

public class EventProcessorSource {
    public static Source<EventOrErrorContext, NotUsed> sourceFrom(EventHubConfig eventHubConfig,
                                                                  BlobStorageConfig blobStorageConfig,
                                                                  int maxBufferedEventContexts) {
        return Source.fromGraph(new EventProcessorSourceGraph(eventHubConfig, blobStorageConfig, maxBufferedEventContexts));
    }
}
