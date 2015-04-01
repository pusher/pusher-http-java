package com.pusher.rest.data;

import java.util.Map;

import com.pusher.rest.util.Marshal;

public class TriggerResult extends Result {
    static class Data {
        public final Map<String, String> eventIds;

        public Data(final Map<String, String> eventIds) {
            this.eventIds = eventIds;
        }
    }

    protected TriggerResult(final Status status, final Integer httpStatus, final String message) {
        super(status, httpStatus, message);
    }

    public static TriggerResult fromResult(final Result result) {
        return new TriggerResult(
            result.getStatus(),
            result.getHttpStatus(),
            result.getMessage()
        );
    }

    /**
     * Get the map from channel name to event ID.
     *
     * If the response status != SUCCESS or when talking to old APIS, the
     * return value will be null.
     */
    public Map<String, String> getEventIDs() {
        if (this.getStatus() != Status.SUCCESS) {
            return null;
        }

        return Marshal.GSON
            .fromJson(this.getMessage(), Data.class)
            .eventIds;
    }
}
