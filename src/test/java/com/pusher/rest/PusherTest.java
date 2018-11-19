package com.pusher.rest;

import static com.pusher.rest.util.Matchers.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import com.pusher.rest.data.Event;

/**
 * Tests which mock the HttpClient to check outgoing requests
 */
public class PusherTest {

    static final String APP_ID = "00001";
    static final String KEY    = "157a2f3df564323a4a73";
    static final String SECRET = "3457a88be87f890dcd98";

    private final Mockery context = new JUnit4Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};

    private CloseableHttpClient httpClient = context.mock(CloseableHttpClient.class);

    private final Pusher p = new Pusher(APP_ID, KEY, SECRET);

    @Before
    public void setup() {
        p.configureHttpClient(new HttpClientBuilder() {
            @Override
            public CloseableHttpClient build() {
                return httpClient;
            }
        });
    }

    /*
     * Serialisation tests
     */

    @SuppressWarnings("unused")
    private static class MyPojo {
        private String aString;
        private int aNumber;

        public MyPojo() {
            this.aString = "value";
            this.aNumber = 42;
        }

        public MyPojo(final String aString, final int aNumber) {
            this.aString = aString;
            this.aNumber = aNumber;
        }
    }

    @Test
    public void initialisePusherValidEncryptionMasterKey() {
        String EncryptionMasterKey = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        Pusher p = new Pusher(APP_ID, KEY, SECRET, EncryptionMasterKey);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initialisePusherInvalidEncryptionMasterKey() {
        String EncryptionMasterKey = "notlongenough";
        Pusher p = new Pusher(APP_ID, KEY, SECRET, EncryptionMasterKey);

    }

    @Test
    public void serialisePojo() throws IOException {
        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(field("data", "{\"aString\":\"value\",\"aNumber\":42}")));
        }});

        p.trigger("my-channel", "event", new MyPojo());
    }

    @Test
    public void customSerialisationGson() throws Exception {
        p.setGsonSerialiser(new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create());

        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(field("data", "{\"a-string\":\"value\",\"a-number\":42}")));
        }});

        p.trigger("my-channel", "event", new MyPojo());
    }

    @Test
    public void customSerialisationByExtension() throws Exception {
        Pusher p = new Pusher(APP_ID, KEY, SECRET) {
            @Override
            protected String serialise(Object data) {
                return (String)data;
            }
        };

        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(field("data", "this is my strong data")));
        }});

        p.trigger("my-channel", "event", "this is my string data");
    }

    @Test
    public void batchEvents() throws IOException {
        final List<Map<String, Object>> res = new ArrayList<Map<String, Object>>() {{
          add(new HashMap<String, Object>() {{
            put("channel", "my-channel");
            put("name", "event-name");
            put("data", "{\"aString\":\"value1\",\"aNumber\":42}");
          }});

          add(new HashMap<String, Object>() {{
            put("channel", "my-channel");
            put("name", "event-name");
            put("data", "{\"aString\":\"value2\",\"aNumber\":43}");
            put("socket_id", "22.33");
          }});
        }};

        context.checking(new Expectations() {{
            oneOf(httpClient).execute(
                with(field("batch", res))
            );
        }});

        List<Event> batch = new ArrayList<Event>();
        batch.add(new Event("my-channel", "event-name", new MyPojo("value1", 42)));
        batch.add(new Event("my-channel", "event-name", new MyPojo("value2", 43), "22.33"));

        p.trigger(batch);
    }

    @Test
    public void mapShouldBeASuitableObjectForData() throws IOException {
        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(field("data", "{\"name\":\"value\"}")));
        }});

        p.trigger("my-channel", "event", Collections.singletonMap("name", "value"));
    }

    @Test
    public void multiLayerMapShouldSerialiseFully() throws IOException {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("k1", "v1");
        Map<String, Object> level2 = new HashMap<String, Object>();
        level2.put("k3", "v3");
        List<String> level3 = new ArrayList<String>();
        level3.add("v4");
        level3.add("v5");
        level2.put("k4", level3);;
        data.put("k2", level2);

        final String expectedData = "{\"k1\":\"v1\",\"k2\":{\"k3\":\"v3\",\"k4\":[\"v4\",\"v5\"]}}";
        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(field("data", expectedData)));
        }});

        p.trigger("my-channel", "event", data);
    }

    @Test
    public void channelList() throws Exception {
        final List<String> channels = Arrays.asList(new String[] { "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten" });

        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(field("channels", channels)));
        }});

        p.trigger(channels, "event", Collections.singletonMap("name", "value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void channelListLimitOverLimit() {
        final List<String> channels = Arrays.asList(new String[] { "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven" });

        p.trigger(channels, "event", Collections.singletonMap("name", "value"));
    }

    @Test
    public void socketIdExclusion() throws Exception {
        final String socketId = "12345.6789";
        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(field("socket_id", socketId)));
        }});

        p.trigger("channel", "event", Collections.singletonMap("name", "value"), socketId);
    }

    @Test
    public void genericGet() throws Exception {
        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(path("/apps/" + APP_ID + "/channels")));
        }});

        p.get("/channels");
    }

    @Test(expected = IllegalArgumentException.class)
    public void reservedParameter() {
        p.get("/channels", Collections.singletonMap("auth_timestamp", "anything"));
    }
}
