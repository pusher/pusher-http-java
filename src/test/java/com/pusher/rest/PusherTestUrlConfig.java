package com.pusher.rest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;

import org.junit.Test;

public class PusherTestUrlConfig {

    @Test
    public void testUrl() throws Exception {
        Pusher p = new Pusher("https://key:secret@api.example.com:4433/apps/00001");

        assertField(p, "scheme", "https");
        assertField(p, "key", "key");
        assertField(p, "secret", "secret");
        assertField(p, "host", "api.example.com:4433");
        assertField(p, "appId", "00001");
    }

    @Test
    public void testUrlNoPort() throws Exception {
        Pusher p = new Pusher("http://key:secret@api.example.com/apps/00001");

        assertField(p, "scheme", "http");
        assertField(p, "key", "key");
        assertField(p, "secret", "secret");
        assertField(p, "host", "api.example.com");
        assertField(p, "appId", "00001");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlMissingField() throws Exception {
        new Pusher("https://key@api.example.com:4433/apps/appId");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlEmptySecret() throws Exception {
        new Pusher("https://key:@api.example.com:4433/apps/appId");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlEmptyKey() throws Exception {
        new Pusher("https://:secret@api.example.com:4433/apps/appId");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlInvalidScheme() throws Exception {
        new Pusher("telnet://key:secret@api.example.com:4433/apps/appId");
    }

    @SuppressWarnings("unchecked")
    private static <V> void assertField(final Object o, final String fieldName, final V expected) throws Exception {
        final Field field = o.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        final V actual = (V)field.get(o);

        assertThat(actual, is(expected));
    }
}
