package com.pusher.rest.data;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;

import com.pusher.rest.data.Result.Status;

public class TriggerResult {
    static class Data {
        public final Map<String, String> eventIds;

        public Data(final Map<String, String> eventIds) {
            this.eventIds = eventIds;
        }
    }

    private static final Gson GSON = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

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
        if (this.status != Status.SUCCESS) {
            return new HashMap<String, String>();
        }

        final Data obj = GSON.fromJson(this.message, Data.class);

        if (obj.eventIds == null) {
            return new HashMap<String, String>();
        }
        
        return obj.eventIds;
    }
}
