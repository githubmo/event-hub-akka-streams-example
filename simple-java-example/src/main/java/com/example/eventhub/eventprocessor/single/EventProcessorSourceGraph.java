package com.example.eventhub.eventprocessor.single;

import akka.japi.function.Procedure;
import akka.stream.Attributes;
import akka.stream.Outlet;
import akka.stream.SourceShape;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.AsyncCallback;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.example.config.BlobStorageConfig;
import com.example.config.EventHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class EventProcessorSourceGraph extends GraphStage<SourceShape<EventOrErrorContext>> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Outlet<EventOrErrorContext> out = Outlet.apply("EventProcessorSource.out");
    private final SourceShape<EventOrErrorContext> shape = SourceShape.of(out);

    private final EventHubConfig eventHubConfig;
    private final BlobContainerAsyncClient blobContainerAsyncClient;

    private final int MaxBufferedEventContexts;

    public EventProcessorSourceGraph(EventHubConfig eventHubConfig,
                                     BlobStorageConfig blobStorageConfig,
                                     int maxBufferedEventContexts
    ) {
        this.eventHubConfig = eventHubConfig;
        this.blobContainerAsyncClient = new BlobContainerClientBuilder()
                .endpoint(blobStorageConfig.connectionString())
                .containerName(blobStorageConfig.container())
                .sasToken(blobStorageConfig.sasToken())
                .buildAsyncClient();
        this.MaxBufferedEventContexts = maxBufferedEventContexts;
    }

    @Override
    public SourceShape<EventOrErrorContext> shape() {
        return shape;
    }

    @Override
    public GraphStageLogic createLogic(Attributes inheritedAttributes) {
        return new GraphStageLogic(shape) {
            private EventProcessorClient eventProcessorClient;

            private final BlockingQueue<EventOrErrorContext> blockingQueue = new LinkedBlockingDeque<>(MaxBufferedEventContexts);
            private final ReentrantLock lock = new ReentrantLock();
            private final Condition shouldPush = lock.newCondition();
            private final Condition isInitialized = lock.newCondition();
            private final AtomicBoolean stopping = new AtomicBoolean(false);
            private AsyncCallback<EventOrErrorContext> pushCallback;

            @Override
            public void preStart() throws Exception {
                pushCallback = createAsyncCallback(
                        (Procedure<EventOrErrorContext>) param -> {
                            push(out, param);
                        }
                );

                // We first acquire the lock and wait for the blockingQueueThread to report that it has entered the lock and is awaiting its signal.
                // Then we can release the lock and allow preStart to finish.
                // This ensures that onPull is not called before blockingQueueThread is awaiting its signal.
                lock.lock();
                try {
                    var blockingQueueThread = new Thread(() -> {
                        final ReentrantLock lock = this.lock;
                        lock.lock();
                        isInitialized.signal();
                        try {
                            while (!stopping.get()) {
                                this.shouldPush.await();
                                var head = blockingQueue.take();
                                pushCallback.invoke(head);
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } finally {
                            lock.unlock();
                        }
                    });
                    blockingQueueThread.start();
                    isInitialized.await();
                } finally {
                    lock.unlock();
                }

                this.eventProcessorClient = new EventProcessorClientBuilder()
                        .connectionString(eventHubConfig.connectionString())
                        .eventHubName(eventHubConfig.eventHubName())
                        .consumerGroup(eventHubConfig.consumerGroup())
                        .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
                        .processEvent(eventContext -> {
//                            logger.info("Queueing an event to send");
                            try {
                                if (eventContext != null) {
                                    blockingQueue.put(new EventOrErrorContext(eventContext));
                                }
                            } catch (InterruptedException e) {
                                logger.error("Internal blocking queue interrupted, which probably means SIGTERM was sent to the application", e);
                            }
//                            logger.info("Event queued to send");
                        })
                        .processError(errorContext -> {
                            if (errorContext != null) {
                                logger.error("EventProcessorClient had a hiccup", errorContext.getThrowable());
                            }
                        })
                        .buildEventProcessorClient();
                eventProcessorClient.start();
            }

            @Override
            public void postStop() throws Exception {
                logger.info("Stopping the EventProcessorSource");
                this.eventProcessorClient.stop();
                stopping.set(true);
                super.postStop();
            }

            {
                setHandler(out,
                        new AbstractOutHandler() {
                            @Override
                            public void onPull() {
                                lock.lock();
                                try {
                                    shouldPush.signal();
                                } finally {
                                    lock.unlock();
                                }
                            }

                            @Override
                            public void onDownstreamFinish() throws Exception, Exception {
                                eventProcessorClient.stop();
                                super.onDownstreamFinish();
                            }

                            @Override
                            public void onDownstreamFinish(Throwable cause) throws Exception, Exception {
                                eventProcessorClient.stop();
                                super.onDownstreamFinish(cause);
                            }
                        });
            }
        };
    }
}
