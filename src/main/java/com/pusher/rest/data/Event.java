package com.pusher.rest.data;

import java.util.List;

/**
 * POJO for JSON encoding of trigger request bodies.
 */
public class Event {

    private final String channel;
    private final String name;
    private final String data;
    private final String socketId;

    public Event(final String channel, final String eventName, final String data, final String socketId) {
        this.channel = channel;
        this.name = eventName;
        this.data = data;
        this.socketId = socketId;
    }

    public String getChannel() {
        return channel;
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
