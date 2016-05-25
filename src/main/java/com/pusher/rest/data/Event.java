package com.pusher.rest.data;

import com.pusher.rest.util.Prerequisites;

/**
 * POJO for JSON encoding of trigger batch events.
 */
public class Event {

    private final String channel;
    private final String name;
    private final Object data;
    private final String socketId;

    public Event(final String channel, final String eventName, final Object data) {
        this(channel, eventName, data, null);
    }

    public Event(final String channel, final String eventName, final Object data, final String socketId) {
        Prerequisites.nonNull("channel", channel);
        Prerequisites.nonNull("eventName", eventName);
        Prerequisites.nonNull("data", data);

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

    public Object getData() {
        return data;
    }

    public String getSocketId() {
        return socketId;
    }
}
