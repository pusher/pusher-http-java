package com.pusher.rest.data;

import com.google.gson.annotations.SerializedName;

public class AuthData {

    private final String auth;
    private final String channelData;
    @SerializedName("shared_secret") private final String sharedSecret;

    /**
     * Private channel constructor
     *
     * @param key App key
     * @param signature Auth signature
     * @param sharedSecret The key that the client should use to decrypt messages sent on private encrypted channels.
     */
    public AuthData(final String key, final String signature, final String sharedSecret) {
        this(key, signature, null, sharedSecret);
    }

    /**
     * Presence channel constructor
     *
     * @param key App key
     * @param signature Auth signature
     * @param channelData Extra user data
     * @param sharedSecret The key that the client should use to decrypt messages sent on private encrypted channels.
     */
    public AuthData(final String key, final String signature, final String channelData, final String sharedSecret) {
        this.auth = key + ":" + signature;
        this.channelData = channelData;
        this.sharedSecret = sharedSecret;
    }

    public String getAuth() {
        return auth;
    }

    public String getChannelData() {
        return channelData;
    }

    public String getSharedSecret() {return sharedSecret; }
}
