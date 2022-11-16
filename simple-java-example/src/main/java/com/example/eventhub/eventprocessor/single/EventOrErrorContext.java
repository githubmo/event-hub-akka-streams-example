package com.example.eventhub.eventprocessor.single;

import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;

public class EventOrErrorContext {
    private final EventContext context;
    private final ErrorContext errorContext;

    private EventOrErrorContext(EventContext context, ErrorContext errorContext) {
        if (context == null && errorContext == null) {
            throw new IllegalArgumentException("One of EventContext or ErrorContext must be provided");
        }
        if (context != null && errorContext != null) {
            throw new IllegalArgumentException("An EventContext and ErrorContext cannot be provided together");
        }
        this.context = context;
        this.errorContext = errorContext;
    }

    public EventOrErrorContext(EventContext context) {
        this(context, null);
    }

    public EventOrErrorContext(ErrorContext errorContext) {
        this(null, errorContext);
    }

    public boolean isError() {
        return this.errorContext != null;
    }

    public EventContext getContext() {
        return context;
    }

    public ErrorContext getErrorContext() {
        return errorContext;
    }
}
