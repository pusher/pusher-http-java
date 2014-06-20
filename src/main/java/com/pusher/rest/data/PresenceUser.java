package com.pusher.rest.data;

import com.pusher.rest.util.Prerequisites;

/**
 * Represents a precence channel "user", that is a user from the domain of your application.
 */
public class PresenceUser {

    private final Object userId;
    private final Object userInfo;

    /**
     * Represents a presence channel user with no additional data associated other than the userId
     */
    public PresenceUser(final String userId) {
        this((Object)userId, null);
    }

    /**
     * Represents a presence channel user with no additional data associated other than the userId
     */
    public PresenceUser(final Number userId) {
        this((Object)userId, null);
    }

    /**
     * Represents a presence channel user and a map of data associated with the user
     */
    public PresenceUser(final String userId, final Object userInfo) {
        this((Object)userId, userInfo);
    }

    /**
     * Represents a presence channel user and a map of data associated with the user
     */
    public PresenceUser(final Number userId, final Object userInfo) {
        this((Object)userId, userInfo);
    }

    /**
     * There's not really a great way to accept either a string or numeric value in a typesafe way,
     * so this will have to do.
     */
    private PresenceUser(final Object userId, final Object userInfo) {
        Prerequisites.nonNull("userId", userId);

        this.userId = userId;
        this.userInfo = userInfo;
    }

    public Object getUserId() {
        return userId;
    }

    public Object getUserInfo() {
        return userInfo;
    }
}
