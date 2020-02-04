package com.pusher.rest;

import com.pusher.rest.data.Result;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.util.HttpConstants;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

/**
 * A library for interacting with the Pusher HTTP API asynchronously.
 * <p>
 * See http://github.com/pusher/pusher-http-java for an overview
 * <p>
 * Essentially:
 * <pre>
 * // Init
 * PusherAsync pusher = new PusherAsync(APP_ID, KEY, SECRET);
 *
 * // Publish
 * CompletableFuture&lt;Result> futureTriggerResult = pusher.trigger("my-channel", "my-eventname", myPojoForSerialisation);
 * triggerResult.thenAccept(triggerResult -> {
 *   if (triggerResult.getStatus() == Status.SUCCESS) {
 *     // request was successful
 *   } else {
 *     // something went wrong with the request
 *   }
 * });
 *
 * // Query
 * CompletableFuture&lt;Result> futureChannelListResult = pusher.get("/channels");
 * futureChannelListResult.thenAccept(triggerResult -> {
 *   if (triggerResult.getStatus() == Status.SUCCESS) {
 *     String channelListAsJson = channelListResult.getMessage();
 *     // etc.
 *   } else {
 *     // something went wrong with the request
 *   }
 * });
 * </pre>
 *
 * See {@link Pusher} for the synchronous implementation.
 */
public class PusherAsync extends PusherAbstract<CompletableFuture<Result>> {

    private AsyncHttpClient client;

    /**
     * Construct an instance of the Pusher object through which you may interact with the Pusher API.
     * <p>
     * The parameters to use are found on your dashboard at https://app.pusher.com and are specific per App.
     * <p>
     *
     * @param appId  The ID of the App you will to interact with.
     * @param key    The App Key, the same key you give to websocket clients to identify your app when they connect to Pusher.
     * @param secret The App Secret. Used to sign requests to the API, this should be treated as sensitive and not distributed.
     */
    public PusherAsync(final String appId, final String key, final String secret) {
        super(appId, key, secret);
        configureHttpClient(config());
    }

    public PusherAsync(final String url) {
        super(url);
        configureHttpClient(config());
    }

    /*
     * CONFIG
     */

    /**
     * Configure the AsyncHttpClient instance which will be used for making calls to the Pusher API.
     * <p>
     * This method allows almost complete control over all aspects of the HTTP client, including
     * <ul>
     * <li>proxy host</li>
     * <li>connection pooling and reuse strategies</li>
     * <li>automatic retry and backoff strategies</li>
     * </ul>
     * <p>
     * e.g.
     * <pre>
     * pusher.configureHttpClient(
     *     config()
     *         .setProxyServer(proxyServer("127.0.0.1", 38080))
     *         .setMaxRequestRetry(5)
     * );
     * </pre>
     *
     * @param builder an {@link DefaultAsyncHttpClientConfig.Builder} with which to configure
     *                the internal HTTP client
     */
    public void configureHttpClient(final DefaultAsyncHttpClientConfig.Builder builder) {
        try {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            // Not a lot useful we can do here
        }

        this.client = asyncHttpClient(builder);
    }

    /*
     * REST
     */

    @Override
    protected CompletableFuture<Result> doGet(URI uri) {
        final Request request = new RequestBuilder(HttpConstants.Methods.GET)
            .setUrl(uri.toString())
            .build();

        return httpCall(request);
    }

    @Override
    protected CompletableFuture<Result> doPost(URI uri, String body) {
        final Request request = new RequestBuilder(HttpConstants.Methods.POST)
                .setUrl(uri.toString())
                .setBody(body)
                .addHeader("Content-Type", "application/json")
                .build();

        return httpCall(request);
    }

    CompletableFuture<Result> httpCall(final Request request) {
        return client
                .prepareRequest(request)
                .execute()
                .toCompletableFuture()
                .thenApply(response -> Result.fromHttpCode(response.getStatusCode(), response.getResponseBody(UTF_8)))
                .exceptionally(Result::fromThrowable);
    }
}
