package com.pusher.rest.data;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.pusher.rest.data.Result;
import static com.pusher.rest.data.Result.Status;

public class TriggerResult {
    static class Data {
        public final Map<String, String> event_ids;

        public Data(final Map<String, String> event_ids) {
            this.event_ids = event_ids;
        }
    }

    private final Result.Status status;
    private final Integer httpStatus;
    private final String message;
    private final Gson dataMarshaller;

    public TriggerResult(final Result result, final Gson dataMarshaller) {
        this.status = result.getStatus();
        this.httpStatus = result.getHttpStatus();
        this.message = result.getMessage();
        this.dataMarshaller = dataMarshaller;
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
        Data obj = this.dataMarshaller.fromJson(this.message, Data.class);
        return obj.event_ids;
    }
}
