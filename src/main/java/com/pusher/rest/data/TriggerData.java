package com.pusher.rest.data;

import java.util.List;

/**
 * POJO for JSON encoding of trigger request bodies.
 */
public class TriggerData {

    private final List<String> channels;
    private final String name;
    private final String data;
    private final String socketId;

    public TriggerData(final List<String> channels, final String eventName, final String data, final String socketId) {
        this.channels = channels;
        this.name = eventName;
        this.data = data;
        this.socketId = socketId;
    }

    public List<String> getChannels() {
        return channels;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    public String getSocketId() {
        return socketId;
    }
}
