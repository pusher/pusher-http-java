package com.pusher.rest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.Test;

import com.pusher.rest.data.Result;
import com.pusher.rest.data.TriggerResult;

public class TriggerResultTest {

    @Test
    public void returnsAnEmptyMapForOldAPI() {
        TriggerResult tr = build(200, "{}");
        assertThat(tr.getEventIDs().isEmpty(), is(true));
    }

    @Test
    public void returnsAChannelMapforNewAPI() {
        TriggerResult tr = build(200, "{\"event_ids\":{\"channel1\":\"eventid1\"}}");
        assertThat(tr.getEventIDs().get("channel1"), is("eventid1"));
        assertThat(tr.getEventIDs().size(), is(1));
    }

    @Test
    public void returnsAnEmptyMapForError() {
        TriggerResult tr = build(500, "{\"event_ids\":{\"channel1\":\"eventid1\"}}");
        assertThat(tr.getEventIDs().isEmpty(), is(true));
    }

    private TriggerResult build(final int status, final String body) {
        Result result = Result.fromHttpCode(status, body);
        return new TriggerResult(result);
    }

}
