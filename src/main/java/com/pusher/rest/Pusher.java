package com.pusher.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.pusher.rest.data.Event;
import com.pusher.rest.data.EventBatch;
import com.pusher.rest.data.PresenceUser;
import com.pusher.rest.data.Result;
import com.pusher.rest.data.TriggerData;
import com.pusher.rest.data.Validity;
import com.pusher.rest.marshaller.DataMarshaller;
import com.pusher.rest.marshaller.DefaultDataMarshaller;
import com.pusher.rest.util.Prerequisites;

/**
 * A library for interacting with the Pusher HTTP API.
 * <p>
 * See http://github.com/pusher/pusher-http-java for an overview
 * <p>
 * Essentially:
 * <pre>
 * // Init
 * Pusher pusher = new Pusher(APP_ID, KEY, SECRET);
 * // Publish
 * Result triggerResult = pusher.trigger("my-channel", "my-eventname", myPojoForSerialisation);
 * if (triggerResult.getStatus() != Status.SUCCESS) {
 *   if (triggerResult.getStatus().shouldRetry()) {
 *     // Temporary, let's schedule a retry
 *   }
 *   else {
 *     // Something is wrong with our request
 *   }
 * }
 *
 * // Query
 * Result channelListResult = pusher.get("/channels");
 * if (channelListResult.getStatus() == Status.SUCCESS) {
 *   String channelListAsJson = channelListResult.getMessage();
 *   // etc
 * }
 * </pre>
 */
public class Pusher {
    private static final Gson BODY_SERIALISER = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private static final Pattern HEROKU_URL = Pattern.compile("(https?)://(.+):(.+)@(.+:?.*)/apps/(.+)");

    private final String appId;
    private final String key;
    private final String secret;

    private String host = "api.pusherapp.com";
    private String scheme = "http";
    private int requestTimeout = 4000; // milliseconds

    private CloseableHttpClient client;
    private DataMarshaller dataMarshaller;

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
        Prerequisites.nonEmpty("appId", appId);
        Prerequisites.nonEmpty("key", key);
        Prerequisites.nonEmpty("secret", secret);
        Prerequisites.isValidSha256Key("secret", secret);

        this.appId = appId;
        this.key = key;
        this.secret = secret;

        configure();
    }

    public Pusher(final String url) {
        Prerequisites.nonNull("url", url);

        final Matcher m = HEROKU_URL.matcher(url);
        if (m.matches()) {
            this.scheme = m.group(1);
            this.key = m.group(2);
            this.secret = m.group(3);
            this.host = m.group(4);
            this.appId = m.group(5);
        }
        else {
            throw new IllegalArgumentException("URL '" + url + "' does not match pattern '<scheme>://<key>:<secret>@<host>[:<port>]/apps/<appId>'");
        }

        Prerequisites.isValidSha256Key("secret", secret);
        configure();
    }

    private void configure() {
        configureHttpClient(defaultHttpClientBuilder());
        this.dataMarshaller = new DefaultDataMarshaller();
    }

    /*
     * CONFIG
     */

    /**
     * For testing or specifying an alternative cluster. See also {@link #setCluster(String)} for the latter.
     * <p>
     * Default: api.pusherapp.com
     *
     * @param host the API endpoint host
     */
    public void setHost(final String host) {
        Prerequisites.nonNull("host", host);

        this.host = host;
    }

    /**
     * For Specifying an alternative cluster.
     * <p>
     * See also {@link #setHost(String)} for targetting an arbitrary endpoint.
     *
     * @param cluster the Pusher cluster to target
     */
    public void setCluster(final String cluster) {
        Prerequisites.nonNull("cluster", cluster);

        this.host = "api-" + cluster + ".pusher.com";
    }

    /**
     * Set whether to use a secure connection to the API (SSL).
     * <p>
     * Authentication is secure even without this option, requests cannot be faked or replayed with access
     * to their plain text, a secure connection is only required if the requests or responses contain
     * sensitive information.
     * <p>
     * Default: false
     *
     * @param encrypted whether to use SSL to contact the API
     */
    public void setEncrypted(final boolean encrypted) {
        this.scheme = encrypted ? "https" : "http";
    }

    /**
     * Default: 4000
     *
     * @param requestTimeout the request timeout in milliseconds
     */
    public void setRequestTimeout(final int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Set the Gson instance used to marshal Objects passed to {@link #trigger(List, String, Object)}
     * Set the marshaller used to serialize Objects passed to {@link #trigger(List, String, Object)}
     * and friends.
     * By default, the library marshals the objects provided to JSON using the Gson library
     * (see https://code.google.com/p/google-gson/ for more details). By providing an instance
     * here, you may exert control over the marshalling, for example choosing how Java property
     * names are mapped on to the field names in the JSON representation, allowing you to match
     * the expected scheme on the client side.
     * We added the {@link #setDataMarshaller(DataMarshaller)} method to allow specification
     * of other marshalling libraries. This method was kept around to maintain backwards
     * compatibility.
     * @param gson a GSON instance configured to your liking
     */
    public void setGsonSerialiser(final Gson gson) {
        setDataMarshaller(new DefaultDataMarshaller(gson));
    }

    /**
     * Set a custom marshaller used to serialize Objects passed to {@link #trigger(List, String, Object)}
     * and friends.
     * <p>
     * By default, the library marshals the objects provided to JSON using the Gson library
     * (see https://code.google.com/p/google-gson/ for more details). By providing an instance
     * here, you may exert control over the marshalling, for example choosing how Java property
     * names are mapped on to the field names in the JSON representation, allowing you to match
     * the expected scheme on the client side.
     *
     * @param marshaller a DataMarshaller instance configured to your liking
     */
    public void setDataMarshaller(final DataMarshaller marshaller) {
        this.dataMarshaller = marshaller;
    }

    /**
     * Returns an HttpClientBuilder with the settings used by default applied. You may apply
     * further configuration (for example an HTTP proxy), override existing configuration
     * (for example, the connection manager which handles connection pooling for reuse) and
     * then call {@link #configureHttpClient(HttpClientBuilder)} to have this configuration
     * applied to all subsequent calls.
     *
     * @see #configureHttpClient(HttpClientBuilder)
     *
     * @return an {@link org.apache.http.impl.client.HttpClientBuilder} with the default settings applied
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
     *
     * @param builder an {@link org.apache.http.impl.client.HttpClientBuilder} with which to configure
     * the internal HTTP client
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
     *     protected String serialise(final Object data) {
     *         return (String)data;
     *     }
     * };
     *
     * pusher.trigger("my-channel", "my-event", "{\"my-data\":\"my-value\"}");
     * </pre>
     *
     * @param data an unserialised event payload
     * @return a serialised event payload
     */
    protected String serialise(final Object data) {
        return dataMarshaller.marshal(data);
    }

    /*
     * REST
     */

    /**
     * Publish a message to a single channel.
     * <p>
     * The message data should be a POJO, which will be serialised to JSON for submission.
     * Use {@link #setDataMarshaller(DataMarshaller)} to control the serialisation
     * <p>
     * Note that if you do not wish to create classes specifically for the purpose of specifying
     * the message payload, use Map&lt;String, Object&gt;. These maps will nest just fine.
     *
     * @param channel the channel name on which to trigger the event
     * @param eventName the name given to the event
     * @param data an object which will be serialised to create the event body
     * @return a {@link Result} object encapsulating the success state and response to the request
     */
    public Result trigger(final String channel, final String eventName, final Object data) {
        return trigger(channel, eventName, data, null);
    }

    /**
     * Publish identical messages to multiple channels.
     *
     * @param channels the channel names on which to trigger the event
     * @param eventName the name given to the event
     * @param data an object which will be serialised to create the event body
     * @return a {@link Result} object encapsulating the success state and response to the request
     */
    public Result trigger(final List<String> channels, final String eventName, final Object data) {
        return trigger(channels, eventName, data, null);
    }

    /**
     * Publish a message to a single channel, excluding the specified socketId from receiving the message.
     *
     * @param channel the channel name on which to trigger the event
     * @param eventName the name given to the event
     * @param data an object which will be serialised to create the event body
     * @param socketId a socket id which should be excluded from receiving the event
     * @return a {@link Result} object encapsulating the success state and response to the request
     */
    public Result trigger(final String channel, final String eventName, final Object data, final String socketId) {
        return trigger(Collections.singletonList(channel), eventName, data, socketId);
    }

    /**
     * Publish identical messages to multiple channels, excluding the specified socketId from receiving the message.
     *
     * @param channels the channel names on which to trigger the event
     * @param eventName the name given to the event
     * @param data an object which will be serialised to create the event body
     * @param socketId a socket id which should be excluded from receiving the event
     * @return a {@link Result} object encapsulating the success state and response to the request
     */
    public Result trigger(final List<String> channels, final String eventName, final Object data, final String socketId) {
        Prerequisites.nonNull("channels", channels);
        Prerequisites.nonNull("eventName", eventName);
        Prerequisites.nonNull("data", data);
        Prerequisites.maxLength("channels", 100, channels);
        Prerequisites.noNullMembers("channels", channels);
        Prerequisites.areValidChannels(channels);
        Prerequisites.isValidSocketId(socketId);

        final String body = BODY_SERIALISER.toJson(new TriggerData(channels, eventName, serialise(data), socketId));

        return post("/events", body);
    }

    /**
     * Publish a batch of different events with a single API call.
     *
     * The batch is limited to 10 events on our multi-tenant clusters.
     *
     * @param batch a list of events to publish
     * @return a {@link Result} object encapsulating the success state and response to the request
     */
    public Result trigger(final List<Event> batch) {
        final List<Event> eventsWithSerialisedBodies = new ArrayList<Event>(batch.size());
        for (final Event e : batch) {
            eventsWithSerialisedBodies.add(
                new Event(
                    e.getChannel(),
                    e.getName(),
                    serialise(e.getData()),
                    e.getSocketId()
                )
            );
        }
        final String body = BODY_SERIALISER.toJson(new EventBatch(eventsWithSerialisedBodies));

        return post("/batch_events", body);
    }

    /**
     * Make a generic HTTP call to the Pusher API.
     * <p>
     * See: http://pusher.com/docs/rest_api
     * <p>
     * NOTE: the path specified here is relative to that of your app. For example, to access
     * the channel list for your app, simply pass "/channels". Do not include the "/apps/[appId]"
     * at the beginning of the path.
     *
     * @param path the path (e.g. /channels) to query
     * @return a {@link Result} object encapsulating the success state and response to the request
     */
    public Result get(final String path) {
        return get(path, Collections.<String, String>emptyMap());
    }

    /**
     * Make a generic HTTP call to the Pusher API.
     * <p>
     * See: http://pusher.com/docs/rest_api
     * <p>
     * Parameters should be a map of query parameters for the HTTP call, and may be null
     * if none are required.
     * <p>
     * NOTE: the path specified here is relative to that of your app. For example, to access
     * the channel list for your app, simply pass "/channels". Do not include the "/apps/[appId]"
     * at the beginning of the path.
     *
     * @param path the path (e.g. /channels) to query
     * @param parameters query parameters to submit with the request
     * @return a {@link Result} object encapsulating the success state and response to the request
     */
    public Result get(final String path, final Map<String, String> parameters) {
        final String fullPath = "/apps/" + appId + path;
        final URI uri = SignatureUtil.uri("GET", scheme, host, fullPath, null, key, secret, parameters);

        return httpCall(new HttpGet(uri));
    }

    /**
     * Make a generic HTTP call to the Pusher API.
     * <p>
     * The body should be a UTF-8 encoded String
     * <p>
     * See: http://pusher.com/docs/rest_api
     * <p>
     * NOTE: the path specified here is relative to that of your app. For example, to access
     * the channel list for your app, simply pass "/channels". Do not include the "/apps/[appId]"
     * at the beginning of the path.
     *
     * @param path the path (e.g. /channels) to submit
     * @param body the body to submit
     * @return a {@link Result} object encapsulating the success state and response to the request
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

    /**
     * If you wanted to send the HTTP API requests manually (e.g. using a different HTTP client), this method
     * will return a java.net.URI which includes all of the appropriate query parameters which sign the request.
     *
     * @param method the HTTP method, e.g. GET, POST
     * @param path the HTTP path, e.g. /channels
     * @param body the HTTP request body, if there is one (otherwise pass null)
     * @return a URI object which includes the necessary query params for request authentication
     */
    public URI signedUri(final String method, final String path, final String body) {
        return signedUri(method, path, body, Collections.<String, String>emptyMap());
    }

    /**
     * If you wanted to send the HTTP API requests manually (e.g. using a different HTTP client), this method
     * will return a java.net.URI which includes all of the appropriate query parameters which sign the request.
     * <p>
     * Note that any further query parameters you wish to be add must be specified here, as they form part of the signature.
     *
     * @param method the HTTP method, e.g. GET, POST
     * @param path the HTTP path, e.g. /channels
     * @param body the HTTP request body, if there is one (otherwise pass null)
     * @param parameters HTTP query parameters to be included in the request
     * @return a URI object which includes the necessary query params for request authentication
     */
    public URI signedUri(final String method, final String path, final String body, final Map<String, String> parameters) {
        return SignatureUtil.uri(method, scheme, host, path, body, key, secret, parameters);
    }

    /*
     * CHANNEL AUTHENTICATION
     */

    /**
     * Generate authentication response to authorise a user on a private channel
     * <p>
     * The return value is the complete body which should be returned to a client requesting authorisation.
     *
     * @param socketId the socket id of the connection to authenticate
     * @param channel the name of the channel which the socket id should be authorised to join
     * @return an authentication string, suitable for return to the requesting client
     */
    public String authenticate(final String socketId, final String channel) {
        Prerequisites.nonNull("socketId", socketId);
        Prerequisites.nonNull("channel", channel);
        Prerequisites.isValidChannel(channel);
        Prerequisites.isValidSocketId(socketId);

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
     * Generate authentication response to authorise a user on a presence channel
     * <p>
     * The return value is the complete body which should be returned to a client requesting authorisation.
     *
     * @param socketId the socket id of the connection to authenticate
     * @param channel the name of the channel which the socket id should be authorised to join
     * @param user a {@link PresenceUser} object which represents the channel data to be associated with the user
     * @return an authentication string, suitable for return to the requesting client
     */
    public String authenticate(final String socketId, final String channel, final PresenceUser user) {
        Prerequisites.nonNull("socketId", socketId);
        Prerequisites.nonNull("channel", channel);
        Prerequisites.nonNull("user", user);
        Prerequisites.isValidChannel(channel);
        Prerequisites.isValidSocketId(socketId);

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

    /**
     * Check the signature on a webhook received from Pusher
     *
     * @param xPusherKeyHeader the X-Pusher-Key header as received in the webhook request
     * @param xPusherSignatureHeader the X-Pusher-Signature header as received in the webhook request
     * @param body the webhook body
     * @return enum representing the possible validities of the webhook request
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
