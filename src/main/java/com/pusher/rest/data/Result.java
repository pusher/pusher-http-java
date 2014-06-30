package com.pusher.rest.data;

import static com.pusher.rest.data.Result.Status.*;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

public class Result {
    public enum Status {
        SUCCESS(false), // No need!
        CLIENT_ERROR(false),
        AUTHENTICATION_ERROR(false),
        MESSAGE_QUOTA_EXCEEDED(false),
        NOT_FOUND(false),
        SERVER_ERROR(true),
        NETWORK_ERROR(true),
        UNKNOWN_ERROR(true),
        ;

        private final boolean shouldRetry;

        private Status(final boolean shouldRetry) {
            this.shouldRetry = shouldRetry;
        }

        /**
         * Whether the call should be retried without modification with an expectation of success
         */
        public boolean shouldRetry() {
            return shouldRetry;
        }
    }

    private final Status status;
    private final Integer httpStatus;
    private final String message;

    private Result(final Status status, final Integer httpStatus, final String message) {
        this.status = status;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public static Result fromHttpCode(final int statusCode, final String responseBody) {
        final Status status;
        switch (statusCode) {
        case 200:
            status = SUCCESS;
            break;
        case 400:
            status = CLIENT_ERROR;
            break;
        case 401:
            status = AUTHENTICATION_ERROR;
            break;
        case 403:
            status = MESSAGE_QUOTA_EXCEEDED;
            break;
        case 404:
            status = NOT_FOUND;
            break;
        default:
            status = statusCode >= 500 && statusCode < 600 ? SERVER_ERROR : UNKNOWN_ERROR;
        }

        return new Result(status, statusCode, responseBody);
    }

    public static Result fromException(final IOException e) {
        return new Result(Status.NETWORK_ERROR, null, e.toString());
    }

    public static Result fromException(final ClientProtocolException e) {
        return new Result(Status.UNKNOWN_ERROR, null, e.toString());
    }

    /**
     * Get the enum classifying the result of the call
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Get the data response (success) or descriptive message (error) returned from the call
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the HTTP status code of the call, useful for debugging instances of UNKNOWN_ERROR
     */
    public Integer getHttpStatus() {
        return httpStatus;
    }
}
