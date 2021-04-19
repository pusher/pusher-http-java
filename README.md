# Pusher Channels Java HTTP library

[![Build Status](https://github.com/pusher/pusher-http-java/workflows/pusher-http-java%20CI/badge.svg)](https://github.com/pusher/pusher-http-java/actions?query=branch%3Amaster)
[![Maven Central](https://img.shields.io/maven-central/v/com.pusher/pusher-http-java.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.pusher%22%20AND%20a:%22pusher-http-java%22)
[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg)](https://raw.githubusercontent.com/pusher/chatkit-android/master/LICENCE.txt)

In order to use this library, you need to have an account on <http://pusher.com/channels>. After registering, you will need the application credentials for your app.

## Supported platforms

* Java SE - supports versions 8, 9, 10 and 11.
* Oracle JDK
* OpenJDK

## Installation

The pusher-http-java library is available in Maven Central:

```
<dependency>
  <groupId>com.pusher</groupId>
  <artifactId>pusher-http-java</artifactId>
  <version>1.2.1</version>
</dependency>
```

## JavaDoc

Javadocs for the latest version are published at <http://pusher.github.io/pusher-http-java/>. Javadoc artifacts are also in Maven and should be available for automatic download and attaching by your IDE.

## Synchronous vs asynchronous

The pusher-http-java library provides two APIs:

- `com.pusher.rest.Pusher`, synchronous, based on Apache HTTP Client (4 series)
- `com.pusher.rest.PusherAsync`, asynchronous, based on [AsyncHttpClient (AHC)](https://github.com/AsyncHttpClient/async-http-client)

The following examples are using `Pusher`, but `PusherAsync` exposes the exact same API, while returning `CompletableFuture<T>` instead of `T`.

## Configuration

The minimum configuration required to use the `Pusher` object are the three constructor arguments which identify your Pusher app. You can find them by going to "API Keys" on your app at <https://dashboard.pusher.com>.

```java
Pusher pusher = new Pusher(appId, apiKey, apiSecret);
```

You then need to specify your app cluster (eg `mt1`/`eu`/`ap1`/`us2`)

```java
pusher.setCluster(<cluster>);
```

### From URL

The basic parameters may also be set from a URL, as provided (for example) as an environment variable when running on Heroku with the Pusher Channels addon:

```java
Pusher pusher = new Pusher("http://<key>:<secret>@api-<cluster>.pusher.com/apps/app_id");
```

Note: the API URL differs depending on the cluster your app was created in. For example:

```
http://<key>:<secret>@api-mt1.pusher.com/apps/app_id
http://<key>:<secret>@api-eu.pusher.com/apps/app_id
```

This form sets the `key`, `secret`, `appId`, `host` and `secure` (based on the protocol in the URL) parameters all at once.

### Additional options

There are additional options which can be set on the `Pusher` object once constructed:

#### Host


If you wish to set a non-standard endpoint, perhaps for testing, you may use `setHost`.

```java
pusher.setHost("api-eu.pusher.com");
```

#### SSL

HTTPS can be used as transport by calling `setEncrypted(true)`. Note that your credentials are not exposed on an unencrypted connection, however the contents of your messages are. Use this option if your messages themselves are sensitive.

#### Advanced HTTP configuration

##### Synchronous library

The synchronous library uses Apache HTTP Client (4 series) internally to make HTTP requests. In order to expose some of the rich and fine configuration available in this component, it is partially exposed. The HttpClient uses the Builder pattern to specify configuration. The Pusher Channels library exposes a method to fetch an `HttpClientBuilder` with sensible defaults, and a method to set the client instance in use to one created by a particular builder. By using these two methods, you can further configure the client, overriding defaults or adding new settings.

For example:

###### HTTP Proxy

To set a proxy:

```java
HttpClientBuilder builder = Pusher.defaultHttpClientBuilder();
builder.setProxy(new HttpHost("proxy.example.com"));
pusher.configureHttpClient(builder);
```

##### Asynchronous library

The asynchronous library uses AsyncHttpClient (AHC) internally to make HTTP requests. Just like the synchronous library, you have a fine-grained control over the HTTP configuration, see https://github.com/AsyncHttpClient/async-http-client. For example:

###### HTTP Proxy

To set a proxy:

```java
pusherAsync.configureHttpClient(
    config()
        .setProxyServer(proxyServer("127.0.0.1", 38080))
        .setMaxRequestRetry(5)
);
```

## Usage

### General info on responses

Requests return a member of the `Result` class. Results have a `getStatus` method returning a member of `Status` which classifies their outcome, for example `Status.SUCCESS` and `Status.AUTHENTICATION_ERROR`. Error `Result`s yield a description from `getMessage`.

### Publishing events

To send an event to one or more channels use the `trigger` method.

The data parameter is serialised using the GSON library (<https://code.google.com/p/google-gson/>) by default. You may
specify your own marshalling library if you wish by using the `setDataMarshaller` method. POJO classes or `java.util.Map`s
are suitable for marshalling.

#### Single channel

```java
pusher.trigger("channel-one", "test_event", Collections.singletonMap("message", "hello world"));
```

#### Multiple channels

```java
List<String> channels = new ArrayList<>();
channels.add("channel-one");
channels.add("channel-two");

pusher.trigger(channels, "test_event", Collections.singletonMap("message", "hello world"));
```

You can trigger an event to at most 10 channels at once. Passing more than 10 channels will cause an exception to be thrown.

#### Excluding event recipients

In order to avoid the client that triggered the event from also receiving it, the `trigger` function takes an optional `socketId` parameter. For more information see: <https://pusher.com/docs/channels/server_api/excluding-event-recipients>.

```java
pusher.trigger(channel, event, data, "1302.1081607");
```

### Authenticating private channels

To authorise your users to access private channels on Channels, you can use the `authenticate` method. This method returns the response body which should be returned to the user requesting authentication.

```java
String authBody = pusher.authenticate(socketId, channel);
```

For more information see: <https://pusher.com/docs/channels/server_api/authenticating-users>

### Authenticating presence channels

Using presence channels is similar to using private channels, but you can specify extra data to identify that particular user. This data is passed as a `PresenceUser` object. As with the message data in `trigger`, the `userInfo` is serialised using GSON.

```java
String userId = "unique_user_id";
Map<String, String> userInfo = new HashMap<>();
userInfo.put("name", "Phil Leggetter");
userInfo.put("twitterId", "@leggetter");

String authBody = pusher.authenticate(socketId, channel, new PresenceUser(userId, userInfo));
```

For more information see: <https://pusher.com/docs/channels/server_api/authenticating-users>

### Application state

It's possible to query the state of the application using the `get` method.

The `path` parameter identifies the resource that the request should be made to and the `parameters` parameter should be a map of additional query string key and value pairs.

For example:

#### Get the list of channels in an application

```java
Result result = pusher.get("/channels", params);
if (result.getStatus() == Status.SUCCESS) {
    String channelListJson = result.getMessage();
    // Parse and act upon list
}
```

Information on the optional `params` and the structure of the returned JSON is defined in the [HTTP API reference](https://pusher.com/docs/channels/library_auth_reference/rest-api#get-channels-fetch-info-for-multiple-channels-).

#### Get the state of a channel

```java
Result result = pusher.get("/channels/[channel_name]");
```

Information on the optional `params` option property and the structure of the returned JSON is defined in the [HTTP API reference](https://pusher.com/docs/channels/library_auth_reference/rest-api#get-channel-fetch-info-for-one-channel-).

#### Get the list of users in a presence channel

```java
Result result = pusher.get("/channels/[channel_name]/users");
```

The `channel_name` in the path must be a [presence channel](https://pusher.com/docs/channels/using_channels/presence-channels). The structure of the returned JSON is defined in the [HTTP API reference](https://pusher.com/docs/channels/library_auth_reference/rest-api#get-users).

### WebHooks

The library provides a simple helper to validate the authenticity of webhooks received from Channels.

Call `validateWebhookSignature` with the values from the `X-Pusher-Key` and `X-Pusher-Signature` headers from the webhook request, and the body as a String. You will receive a member of `Validity` indicating whether the webhook could be validated or not.

### Generating HTTP API signatures

If you wanted to send the HTTP API requests manually (e.g. using a different HTTP client), you can use the `signedUri` method to generate a java.net.URI for your request which contains the necessary signatures:

```java
URI requestUri = pusher.signedUri("GET", "/apps/<appId>/channels", null); // no body on GET request
```

Note that the URI does not include the body parameter, however the signature contains a digest of it, so the body must be sent with the request as it was presented at signature time:

```java
URI requestUri = pusher.signedUri("POST", "/apps/<appId>/events", body);

PostRequest request = new PostRequest(requestUri);
request.setBody(body);
request.execute();
```

Additional query params may be passed to be added to the URI:

```java
URI requestUri = pusher.signedUri("GET", "/apps/<appId>/channels", null, Collections.singletonMap("filter_by_prefix", "myprefix-"));
```

Query parameters can't contain following keys, as they are used to sign the request:

- auth_key
- auth_timestamp
- auth_version
- auth_signature
- body_md5

### Multi-threaded usage with the synchronous library

The library is threadsafe and intended for use from many threads simultaneously. By default, HTTP connections are persistent and a pool of open connections is maintained. This re-use reduces the overhead involved in repeated TCP connection establishments and teardowns.

IO calls are blocking, and if more threads make requests at one time than the configured maximum pool size, they will wait for a connection to become idle. By default there are at most 2 concurrent connections maintained. This should be enough to many use cases, but it can be configured:

```java
PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
connManager.setDefaultMaxPerRoute(maxConns);

pusher.configureHttpClient(
    Pusher.defaultHttpClientBuilder()
          .setConnectionManager(connManager)
);
```

## License

This code is free to use under the terms of the MIT license.
