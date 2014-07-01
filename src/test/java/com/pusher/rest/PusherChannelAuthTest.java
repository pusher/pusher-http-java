package com.pusher.rest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.Test;

import com.pusher.rest.data.PresenceUser;

public class PusherChannelAuthTest {

    private final Pusher p = new Pusher("00001", "278d425bdf160c739803", "7ad3773142a6692b25b8");

    @Test
    public void privateChannelAuth() {
        assertThat(p.authenticate("1234.1234", "private-foobar"),
                is("{\"auth\":\"278d425bdf160c739803:58df8b0c36d6982b82c3ecf6b4662e34fe8c25bba48f5369f135bf843651c3a4\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void privateChannelWrongPrefix() {
        p.authenticate("1234.1234", "presence-foobar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void privateChannelNoPrefix() {
        p.authenticate("1234.1234", "foobar");
    }

    @Test
    public void presenceChannelAuth() {
        assertThat(p.authenticate("1234.1234", "presence-foobar", new PresenceUser(Integer.valueOf(10), Collections.singletonMap("name", "Mr. Pusher"))),
                is("{\"auth\":\"278d425bdf160c739803:afaed3695da2ffd16931f457e338e6c9f2921fa133ce7dac49f529792be6304c\",\"channel_data\":\"{\\\"user_id\\\":10,\\\"user_info\\\":{\\\"name\\\":\\\"Mr. Pusher\\\"}}\"}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void presenceChannelWrongPrefix() {
        p.authenticate("1234.1234", "private-foobar", new PresenceUser("dave"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void presenceChannelNoPrefix() {
        p.authenticate("1234.1234", "foobar", new PresenceUser("dave"));
    }
}
