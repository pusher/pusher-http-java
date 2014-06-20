package com.pusher.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.pusher.rest.data.AuthData;
import com.pusher.rest.data.PresenceUser;
import com.pusher.rest.data.Result;
import com.pusher.rest.data.TriggerData;
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
    private int requestTimeout = 4000; // milliseconds

    private CloseableHttpClient client;
    private Gson dataMarshaller;

    /**
     * Construct an instance of the Pusher object through which you may interact with the Pusher API.
     * <p>
     * The parameters to use are found on your dashboard at https://app.pusher.com and are specific per App.
     * <p>
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

        this.configureHttpClient(defaultHttpClientBuilder());

        this.dataMarshaller = new Gson();
    }

    /*
     * CONFIG
     */

    /**
     * Set the API endpoint host.
     * <p>
     * For testing or specifying an alternative cluster.
     * <p>
     * Default: api.pusherapp.com
     */
    public void setHost(final String host) {
        Prerequisites.nonNull("host", host);

        this.host = host;
    }

    /**
     * Set whether to use a secure connection to the API (SSL).
     * <p>
     * Authentication is secure even without this option, requests cannot be faked or replayed with access
     * to their plain text, a secure connection is only required if the requests or responses contain
     * sensitive information.
     * <p>
     * Default: false
     */
    public void setEncrypted(final boolean encrypted) {
        this.scheme = encrypted ? "https" : "http";
    }

    /**
     * Set the request timeout in milliseconds
     * <p>
     * Default 4000
     */
    public void setRequestTimeout(final int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Set the Gson instance used to marshall Objects passed to {@link #trigger(List, String, Object)}
     * and friends.
     * <p>
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
     * Returns an HttpClientBuilder with the settings used by default applied. You may apply
     * further configuration (for example an HTTP proxy), override existing configuration
     * (for example, the connection manager which handles connection pooling for reuse) and
     * then call {@link #configureHttpClient(HttpClientBuilder)} to have this configuration
     * applied to all subsequent calls.
     *
     * @see #configureHttpClient(HttpClientBuilder)
     */
    public static HttpClientBuilder defaultHttpClientBuilder() {
        return HttpClientBuilder.create()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .setConnectionReuseStrategy(new DefaultConnectionReuseStrategy())
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                .disableRedirectHandling();
    }

    /**
     * Configure the HttpClient instance which will be used for making calls to the Pusher API.
     * <p>
     * This method allows almost complete control over all aspects of the HTTP client, including
     * <ul>
     * <li>proxy host</li>
     * <li>connection pooling and reuse strategies</li>
     * <li>automatic retry and backoff strategies</li>
     * </ul>
     * It is <strong>strongly</strong> recommended that you take the value of {@link #defaultHttpClientBuilder()}
     * as a base, apply your custom config to that and then pass the builder in here, to ensure
     * that sensible defaults for configuration areas you are not setting explicitly are retained.
     * <p>
     * e.g.
     * <pre>
     * pusher.configureHttpClient(
     *     Pusher.defaultHttpClientBuilder()
     *           .setProxy(new HttpHost("proxy.example.com"))
     *           .disableAutomaticRetries()
     * );
     * </pre>
     *
     * @see #defaultHttpClientBuilder()
     */
    public void configureHttpClient(final HttpClientBuilder builder) {
        try {
            if (client != null) client.close();
        }
        catch (IOException e) {
            // Not a lot useful we can do here
        }

        this.client = builder.build();
    }

    /**
     * This method provides an override point if the default Gson based serialisation is absolutely
     * unsuitable for your use case, even with customisation of the Gson instance doing the serialisation.
     * <p>
     * For example, in the simplest case, you might already have your data pre-serialised and simply want
     * to elide the default serialisation:
     * <pre>
     * Pusher pusher = new Pusher(appId, key, secret) {
     *     &commat;Override
     *     protected String serialise(final Object data) {
     *         return (String)data;
     *     }
     * };
     *
     * pusher.trigger("my-channel", "my-event", "{\"my-data\":\"my-value\"}");
     * </pre>
     */
    protected String serialise(final Object data) {
        return dataMarshaller.toJson(data);
    }

    /*
     * REST
     */

    /**
     * Publish a message to a single channel.
     * <p>
     * The message data should be a POJO, which will be serialised to JSON for submission.
     * Use {@link #setGsonSerialiser(Gson)} to control the serialisation
     * <p>
     * Note that if you do not wish to create classes specifically for the purpose of specifying
     * the message payload, use Map&lt;String, Object&gt;. These maps will nest just fine.
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

        final String body = BODY_SERIALISER.toJson(new TriggerData(channels, eventName, serialise(data), socketId));

        return post("/events", body);
    }

    /**
     * Make a generic REST call to the Pusher API.
     * <p>
     * See: http://pusher.com/docs/rest_api
     * <p>
     * Parameters should be a map of query parameters for the REST call, and may be null
     * if none are required.
     * <p>
     * NOTE: the path specified here is relative to that of your app. For example, to access
     * the channel list for your app, simply pass "/channels". Do not include the "/apps/[appId]"
     * at the beginning of the path.
     */
    public Result get(final String path, final Map<String, String> parameters) {
        final String fullPath = "/apps/" + appId + path;
        final URI uri = SignatureUtil.uri("GET", scheme, host, fullPath, null, key, secret, parameters);

        return httpCall(new HttpGet(uri));
    }

    /**
     * Make a generic REST call to the Pusher API.
     * <p>
     * The body should be a UTF-8 encoded String
     * <p>
     * See: http://pusher.com/docs/rest_api
     * <p>
     * NOTE: the path specified here is relative to that of your app. For example, to access
     * the channel list for your app, simply pass "/channels". Do not include the "/apps/[appId]"
     * at the beginning of the path.
     */
    public Result post(final String path, final String body) {
        final String fullPath = "/apps/" + appId + path;
        final URI uri = SignatureUtil.uri("POST", scheme, host, fullPath, body, key, secret, Collections.<String, String>emptyMap());

        final StringEntity bodyEntity = new StringEntity(body, "UTF-8");
        bodyEntity.setContentType("application/json");

        final HttpPost request = new HttpPost(uri);
        request.setEntity(bodyEntity);

        return httpCall(request);
    }

    Result httpCall(final HttpRequestBase request) {
        final RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(requestTimeout)
                .setConnectionRequestTimeout(requestTimeout)
                .setConnectTimeout(requestTimeout)
                .build();
        request.setConfig(config);

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

    /*
     * CHANNEL AUTHENTICATION
     */

    /**
     * Generate authentication response to authorise a user on a private channel
     *
     * The return value is the complete body which should be returned to a client requesting authorisation.
     */
    public String authenticate(final String channel, final String socketId) {
        Prerequisites.nonNull("socketId", socketId);
        Prerequisites.nonNull("channel", channel);

        if (channel.startsWith("presence-")) {
            throw new IllegalArgumentException("This method is for private channels, use authenticate(String, String, PresenceUser) to authenticate for a presence channel.");
        }
        if (!channel.startsWith("private-")) {
            throw new IllegalArgumentException("Authentication is only applicable to private and presence channels");
        }

        final String signature = SignatureUtil.sign(socketId + ":" + channel, secret);
        return BODY_SERIALISER.toJson(new AuthData(key, signature));
    }

    /**
     * Generate authentication response to authorise a user on a private channel
     *
     * The return value is the complete body which should be returned to a client requesting authorisation.
     */
    public String authenticate(final String channel, final String socketId, final PresenceUser user) {
        Prerequisites.nonNull("user", user);
        Prerequisites.nonNull("channel", channel);

        if (channel.startsWith("private-")) {
            throw new IllegalArgumentException("This method is for presence channels, use authenticate(String, String) to authenticate for a private channel.");
        }
        if (!channel.startsWith("presence-")) {
            throw new IllegalArgumentException("Authentication is only applicable to private and presence channels");
        }

        final String channelData = BODY_SERIALISER.toJson(user);
        final String signature = SignatureUtil.sign(socketId + ":" + channel + ":" + channelData, secret);
        return BODY_SERIALISER.toJson(new AuthData(key, signature, channelData));
    }

    /*
     * WEBHOOK VALIDATION
     */

    public Validity validateWebhookSignature(final String xPusherKeyHeader, final String xPusherSignatureHeader, final String body) {
        if (!xPusherKeyHeader.trim().equals(key)) {
            // We can't validate the signature, because it was signed with a different key to the one we were initialised with.
            return Validity.SIGNED_WITH_WRONG_KEY;
        }

        final String recalculatedSignature = SignatureUtil.sign(body, secret);
        return xPusherSignatureHeader.trim().equals(recalculatedSignature) ? Validity.VALID : Validity.INVALID;
    }
}
