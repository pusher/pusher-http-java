package com.pusher.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Field;

import com.pusher.rest.util.PusherNoHttp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PusherTestUrlConfig {

    @Test
    public void testUrl() throws Exception {
        PusherNoHttp p = new PusherNoHttp("https://key:secret@api.example.com:4433/apps/00001");

        assertField(p, "scheme", "https");
        assertField(p, "key", "key");
        assertField(p, "secret", "secret");
        assertField(p, "host", "api.example.com:4433");
        assertField(p, "appId", "00001");
    }

    @Test
    public void testUrlNoPort() throws Exception {
        PusherNoHttp p = new PusherNoHttp("http://key:secret@api.example.com/apps/00001");

        assertField(p, "scheme", "http");
        assertField(p, "key", "key");
        assertField(p, "secret", "secret");
        assertField(p, "host", "api.example.com");
        assertField(p, "appId", "00001");
    }

    @Test
    public void testUrlMissingField() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new PusherNoHttp("https://key@api.example.com:4433/apps/appId");
        });
    }

    @Test
    public void testUrlEmptySecret() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new PusherNoHttp("https://key:@api.example.com:4433/apps/appId");
        });
    }

    @Test
    public void testUrlEmptyKey() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new PusherNoHttp("https://:secret@api.example.com:4433/apps/appId");
        });
    }

    @Test
    public void testUrlInvalidScheme() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new PusherNoHttp("telnet://key:secret@api.example.com:4433/apps/appId");
        });
    }

    private static <T extends PusherAbstract<?>, V> void assertField(final T o, final String fieldName, final V expected) throws Exception {
        final Field field = PusherAbstract.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        final V actual = (V)field.get(o);

        assertThat(actual, is(expected));
    }
}
