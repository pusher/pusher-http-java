package com.pusher.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SignedRequestTest {

    @Test
    public void stringToSignFields() {
        assertThat(SignedRequest.buildSignatureString("POST", "/a/path", Collections.singletonMap("k", "v")),
                is("POST\n/a/path\nk=v"));

        assertThat(SignedRequest.buildSignatureString("GET", "/a/nother/path", Collections.singletonMap("K", "V")),
                is("GET\n/a/nother/path\nK=V"));
    }

    @Test
    public void stringToSignQueryParamsOrderedByKey() {
        Map<String, String> params = new HashMap<String, String>();
        params.put("a", "v1");
        params.put("bat", "v2");
        params.put("car", "v3");
        params.put("cat", "v4");

        // Make sure the iteration order is not incidentally alphabetical, or we're not testing what we intend to.
        String[] defaultOrder = params.keySet().toArray(new String[0]);
        String[] sortedOrder = params.keySet().toArray(new String[0]);
        Arrays.sort(sortedOrder);
        assertThat(defaultOrder, not(equalTo(sortedOrder)));

        String toSign = SignedRequest.buildSignatureString("POST", "/", params);
        assertThat(toSign, containsString("a=v1&bat=v2&car=v3&cat=v4"));
    }

}
