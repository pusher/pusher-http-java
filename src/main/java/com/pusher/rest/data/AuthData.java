package com.pusher.rest.data;

public class AuthData {

    private final String auth;
    private final String channelData;

    /**
     * Private channel constructor
     */
    public AuthData(final String key, final String signature) {
        this(key, signature, null);
    }

    /**
     * Presence channel constructor
     */
    public AuthData(final String key, final String signature, final String channelData) {
        this.auth = key + ":" + signature;
        this.channelData = channelData;
    }

    public String getAuth() {
        return auth;
    }

    public String getChannelData() {
        return channelData;
    }
}
