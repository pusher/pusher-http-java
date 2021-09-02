package com.pusher.rest;

public class PusherException extends RuntimeException {

    public PusherException(String errorMessage) {
        super(errorMessage);
    }

    public static PusherException encryptionMasterKeyRequired() {
        return new PusherException("You cannot use encrypted channels without setting a master encryption key");
    }

    public static PusherException cannotTriggerMultipleChannelsWithEncryption() {
        return new PusherException("You cannot trigger to multiple channels when using encrypted channels");
    }
}
