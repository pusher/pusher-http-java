package com.pusher.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pusher.rest.util.Prerequisites;

public class Pusher {
    private static final Gson BODY_SERIALISER = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final String appId;
    private final String key;
    private final String secret;

    private String host = "api.pusherapp.com";
    private String scheme = "http";

    private HttpClient client;
    private Gson dataMarshaller;

    /**
     * Construct an instance of the Pusher object through which you may interact with the Pusher API.
     *
     * The parameters to use are found on your dashboard at https://app.pusher.com and are specific per App.
     *
     * @param appId The ID of the App you will to interact with.
     * @param key The App Key, the same key you give to websocket clients to identify your app when they connect to Pusher.
     * @param secret The App Secret. Used to sign requests to the API, this should be treated as sensitive and not distributed.
     */
    public Pusher(final String appId, final String key, final String secret) {
        Prerequisites.nonNull("appId", appId);
        Prerequisites.nonNull("key", key);
        Prerequisites.nonNull("secret", secret);
        Prerequisites.isValidSha256Key("secret", secret);

        this.appId = appId;
        this.key = key;
        this.secret = secret;

        this.client = HttpClientBuilder.create()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .build();

        this.dataMarshaller = new Gson();
    }

    /**
     * Set the API endpoint host.
     *
     * For testing or specifying an alternative cluster.
     *
     * Default: api.pusherapp.com
     */
    public void setHost(final String host) {
        Prerequisites.nonNull("host", host);

        this.host = host;
    }

    /**
     * Set whether to use a secure connection to the API (SSL).
     *
     * Authentication is secure even without this option, requests cannot be faked or replayed with access
     * to their plain text, a secure connection is only required if the requests or responses contain
     * sensitive information.
     *
     * Default: false
     */
    public void setSecure(final boolean secure) {
        this.scheme = secure ? "https" : "http";
    }

    /**
     * Set the Gson instance used to marshall Objects passed to #trigger
     *
     * The library marshalls the objects provided to JSON using the Gson library
     * (see https://code.google.com/p/google-gson/ for more details). By providing an instance
     * here, you may exert control over the marshalling, for example choosing how Java property
     * names are mapped on to the field names in the JSON representation, allowing you to match
     * the expected scheme on the client side.
     */
    public void setGsonSerialiser(final Gson gson) {
        this.dataMarshaller = gson;
    }

    /**
     * Publish a message to a single channel.
     *
     * The message data should be a POJO, which will be serialised to JSON for submission.
     * Use #setGsonSerialiser(Gson) to control the serialisation
     *
     * Note that if you do not wish to create classes specifically for the purpose of specifying
     * the message payload, use Map<String, Object>. These maps will nest just fine.
     */
    public Result trigger(final String channel, final String eventName, final Object data) {
        return trigger(channel, eventName, data, null);
    }

    /**
     * Publish identical messages to multiple channels.
     */
    public Result trigger(final List<String> channels, final String eventName, final Object data) {
        return trigger(channels, eventName, data, null);
    }

    /**
     * Publish a message to a single channel, excluding the specified socketId from receiving the message.
     */
    public Result trigger(final String channel, final String eventName, final Object data, final String socketId) {
        return trigger(Collections.singletonList(channel), eventName, data, socketId);
    }

    /**
     * Publish identical messages to multiple channels, excluding the specified socketId from receiving the message.
     */
    public Result trigger(final List<String> channels, final String eventName, final Object data, final String socketId) {
        Prerequisites.nonNull("channels", channels);
        Prerequisites.nonNull("eventName", eventName);
        Prerequisites.nonNull("data", data);
        Prerequisites.maxLength("channels", 10, channels);
        Prerequisites.noNullMembers("channels", channels);

        final String path = "/apps/" + appId + "/events";
        final String body = BODY_SERIALISER.toJson(new TriggerData(channels, eventName, serialise(data), socketId));

        return httpCall(path, body);
    }

    Result httpCall(final String path, final String body) {
        final URI uri = SignedRequest.uri("POST", scheme, host, path, body, key, secret, Collections.<String, String>emptyMap());

        final StringEntity bodyEntity = new StringEntity(body, "UTF-8");
        bodyEntity.setContentType("application/json");

        final HttpPost request = new HttpPost(uri);
        request.setEntity(bodyEntity);

        try {
            final HttpResponse response = client.execute(request);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            response.getEntity().writeTo(baos);
            final String responseBody = new String(baos.toByteArray(), "UTF-8");

            return Result.fromHttpCode(response.getStatusLine().getStatusCode(), responseBody);
        }
        catch (final IOException e) {
            return Result.fromException(e);
        }
    }

    /**
     * This method provides an override point if the default Gson based serialisation is absolutely
     * unsuitable for your use case, even with customisation of the Gson instance doing the serialisation.
     */
    protected String serialise(final Object data) {
        return dataMarshaller.toJson(data);
    }

    // Inject mock for testing
    void setHttpClient(final HttpClient client) {
        this.client = client;
    }
}
