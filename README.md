# Pusher Java REST library

In order to use this library, you need to have an account on <http://pusher.com>. After registering, you will need the application credentials for your app.

## Installation

TODO: Maven snippet

## Configuration

The minimum configureation required to use the `Pusher` object are the three constructor arguments which identify your Pusher app. You can find them by going to "API Keys" on your app at <https://app.pusher.com>.

```java
Pusher pusher = new Pusher(appId, apiKey, apiSecret);
```

### Additional options

There are additional options wich can be set on the `Pusher` object once constructed:

#### Cluster or host

If you are using an alternative Pusher cluster, you can provide its name to the `setCluster` method.

If you wish to set a non-standard endpoint, perhaps for testing, you may use `setHost`.

```java
pusher.setCluster("eu");
// OR, equivalent but more flexible:
pusher.setHost("api-eu.pusher.com");
```

#### Timeouts

The default timeout is 4 seconds. A timeout in milliseconds to be applied to socket opens and reads can be applied, for example 10 seconds:

```java
pusher.setRequestTimeout(10000);
```

#### SSL

HTTPS can be used as transport by calling `setSecure(true)`. Note that your credentials are not exposed on an insecure connection, however the contents of your messages are. Use this option if your messages themselves are sensitive.

## Usage

### General info on responses

Requests return a member of the `Result` class. Results have a `getStatus` method returning a member of `Status` which classifies their outcome, for example `Status.SUCCESS` and `Status.AUTHENTICATION_ERROR`. Error `Result`s yield a description from `getMessage`.

### Publishing events

To send an event to one or more channels use the `trigger` method.

The data parameter is serialised using the GSON library (<https://code.google.com/p/google-gson/>). POJO data structures or `java.util.Map`s are suitable.

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

### Excluding event recipients

In order to avoid the client that triggered the event from also receiving it, the `trigger` function takes an optional `socketId` parameter. For more information see: <http://pusher.com/docs/publisher_api_guide/publisher_excluding_recipients>.

```java
pusher.trigger(channel, event, data, "1302.1081607");
```

### Authenticating private channels

To authorise your users to access private channels on Pusher, you can use the `authenticate` method. This method returns the response body which should be returned to the user requesting authentication.

```java
String authBody = pusher.authenticate(channel, socketId);
```

For more information see: <http://pusher.com/docs/authenticating_users>

### Authenticating presence channels

Using presence channels is similar to private channels, but you can specify extra data to identify that particular user. This data is passed as a `PresenceUser` object. As with the message data in `trigger`, the `userInfo` is serialised using GSON.

```java
String userId = "unique_user_id";
Map<String, String> userInfo = new HashMap<>();
userInfo.add("name", "Phil Leggetter");
userInfo.add("twitterId", "@leggetter");

String authBody = pusher.auth(channel, socketId, new PresenceUser(userId, userInfo));
```

For more information see: <http://pusher.com/docs/authenticating_users>

### Application state

It's possible to query the state of the application using the `get` method.

The `path` parameter identifies the resource that the request should be made to and the `parameters` parameter should be a map of additional query string key and value pairs.

Params can't include following keys:
- auth_key
- auth_timestamp
- auth_version
- auth_signature
- body_md5

For example:

#### Get the list of channels in an application

```java
Result result = pusher.get("/channels", params);
if (result.getStatus == Status.SUCCESS) {
    String channelListJson = result.getMessage();
    // Parse and act upon list
}
```

Information on the optional `params` and the structure of the returned JSON is defined in the [REST API reference](http://pusher.com/docs/rest_api#method-get-channels).

#### Get the state of a channel

```java
Result result = pusher.get("/channels/[channel_name]");
```

Information on the optional `params` option property and the structure of the returned JSON is defined in the [REST API reference](http://pusher.com/docs/rest_api#method-get-channel).

#### Get the list of users in a presence channel

```java
Result result = pusher.get("/channels/[channel_name]/users");
```

The `channel_name` in the path must be a [presence channel](http://pusher.com/docs/presence). The structure of the returned JSON is defined in the [REST API reference](http://pusher.com/docs/rest_api#method-get-users).

### WebHooks

The library provides a simple helper to validate the authenticity of webhooks received from Pusher.

Call `validateWebhookSignature` with the values from the `X-Pusher-Key` and `X-Pusher-Signature` headers from the webhook request, and the body as a String. You will receive a member of `Validity` indicating whether the webhook could be validated or not.

### Generating REST API signatures

If you wanted to send the REST API requests manually (e.g. using a different HTTP client), you can use the `signedUri` method to generate a java.net.URI for your request which contains the necessary signatures:

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

## License

This code is free to use under the terms of the MIT license.
