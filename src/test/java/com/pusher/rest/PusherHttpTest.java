package com.pusher.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.pusher.rest.Result.Status;

/**
 * Tests which use a local server to check response handling
 */
public class PusherHttpTest {

    private LocalTestServer server;
    private HttpGet request;

    private int responseStatus = 200;
    private String responseBody;

    private Pusher p;

    @Before
    public void setup() throws Exception {
        server = new LocalTestServer(null, null);
        server.register("/*", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                response.setStatusCode(responseStatus);
                if (responseBody != null) {
                    response.setEntity(new StringEntity(responseBody));
                }
            }
        });

        server.start();
        request = new HttpGet("http://" + server.getServiceAddress().getHostName() + ":" + server.getServiceAddress().getPort() + "/test");

        p = new Pusher(PusherTest.APP_ID, PusherTest.KEY, PusherTest.SECRET);
    }

    @After
    public void teardown() throws Exception {
        server.stop();
    }

    @Test
    public void successReturnsOkAndBody() throws Exception {
        responseStatus = 200;
        responseBody = "{}";

        Result result = p.httpCall(request);
        assertThat(result.getStatus(), is(Status.OK));
        assertThat(result.getMessage(), is(responseBody));
    }

    @Test
    public void status400ReturnsGenericErrorAndMessage() throws Exception {
        responseStatus = 400;
        responseBody = "A lolcat got all up in ur request";

        Result result = p.httpCall(request);
        assertThat(result.getStatus(), is(Status.CLIENT_ERROR));
        assertThat(result.getMessage(), is(responseBody));
    }

    @Test
    public void status401ReturnsAuthenticationErrorAndMessage() throws Exception {
        responseStatus = 401;
        responseBody = "Sorry, not in those shoes";

        Result result = p.httpCall(request);
        assertThat(result.getStatus(), is(Status.AUTHENTICATION_ERROR));
        assertThat(result.getMessage(), is(responseBody));
    }

    @Test
    public void status403ReturnsAuthenticationErrorAndMessage() throws Exception {
        responseStatus = 403;
        responseBody = "Sorry, not with all those friends";

        Result result = p.httpCall(request);
        assertThat(result.getStatus(), is(Status.MESSAGE_QUOTA_EXCEEDED));
        assertThat(result.getMessage(), is(responseBody));
    }

    @Test
    public void status404ReturnsNotFoundErrorAndMessage() throws Exception {
        responseStatus = 404;
        responseBody = "This is not the endpoint you are looking for";

        Result result = p.httpCall(request);
        assertThat(result.getStatus(), is(Status.NOT_FOUND));
        assertThat(result.getMessage(), is(responseBody));
    }

    @Test
    public void status500ReturnsServerErrorAndMessage() throws Exception {
        responseStatus = 500;
        responseBody = "Gary? Gary! It's on fire!!";

        Result result = p.httpCall(request);
        assertThat(result.getStatus(), is(Status.SERVER_ERROR));
        assertThat(result.getMessage(), is(responseBody));
    }

    @Test
    public void status503ReturnsServerErrorAndMessage() throws Exception {
        responseStatus = 503;
        responseBody = "Gary, did you restart all the back-ends at once?!";

        Result result = p.httpCall(request);
        assertThat(result.getStatus(), is(Status.SERVER_ERROR));
        assertThat(result.getMessage(), is(responseBody));
    }

    @Test
    public void connectionRefusedReturnsNetworkError() throws Exception {
        server.stop(); // don't listen for this test

        Result result = p.httpCall(request);
        assertThat(result.getStatus(), is(Status.NETWORK_ERROR));
        assertThat(result.getMessage(), containsString("Connection refused"));
    }
}
