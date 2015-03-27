package com.pusher.rest.data;

import java.util.Map;

import com.google.gson.Gson;

public class TriggerResult {
    static class Data {
        public final Map<String, String> event_ids;

        public Data(final Map<String, String> event_ids) {
            this.event_ids = event_ids;
        }
    }

    private static final Gson GSON = new Gson();

    private final Result.Status status;
    private final Integer httpStatus;
    private final String message;

    public TriggerResult(final Result result) {
        this.status = result.getStatus();
        this.httpStatus = result.getHttpStatus();
        this.message = result.getMessage();
    }

    /**
     * Get the enum classifying the result of the call
     */
    public Result.Status getStatus() {
        return status;
    }

    /**
     * Get the HTTP status code of the call, useful for debugging instances of UNKNOWN_ERROR
     */
    public Integer getHttpStatus() {
        return httpStatus;
    }

    /**
     * Get the data response (success) or descriptive message (error) returned from the call
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the map from channel name to event ID.
     *
     * Response might be null.
     */
    public Map<String, String> getEventIDs() {
        final Data obj = GSON.fromJson(this.message, Data.class);
        return obj.event_ids;
    }
}
