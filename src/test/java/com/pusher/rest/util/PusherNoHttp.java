package com.pusher.rest.util;

import com.pusher.rest.PusherAbstract;

import java.net.URI;

public class PusherNoHttp extends PusherAbstract<Object> {

    public PusherNoHttp(final String appId, final String key, final String secret) {
        super(appId, key, secret);
    }

    public PusherNoHttp(final String url) {
        super(url);
    }

    @Override
    protected Object doGet(final URI uri) {
        throw new IllegalStateException("Shouldn't have been called, HTTP level not implemented");
    }

    @Override
    protected Object doPost(final URI uri, final String body) {
        throw new IllegalStateException("Shouldn't have been called, HTTP level not implemented");
    }

}
