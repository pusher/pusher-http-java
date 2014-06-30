package com.pusher.rest.util;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.http.client.methods.HttpPost;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import com.google.gson.Gson;

public class Matchers {

    public static <T> Matcher<HttpPost> field(final String fieldName, final T expected) {
        return new TypeSafeDiagnosingMatcher<HttpPost>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("HTTP request with field [" + fieldName + "], value [" + expected + "] in JSON body");
            }

            @Override
            public boolean matchesSafely(HttpPost item, Description mismatchDescription) {
                try {
                    @SuppressWarnings("unchecked")
                    T actual = (T)new Gson().fromJson(retrieveBody(item), Map.class).get(fieldName);
                    mismatchDescription.appendText("value was [" + actual + "]");
                    return expected.equals(actual);
                }
                catch (Exception e) {
                    return false;
                }
            }
        };
    }

    public static Matcher<HttpPost> stringBody(final String expected) {
        return new TypeSafeDiagnosingMatcher<HttpPost>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("HTTP request with body [" + expected + "]");
            }

            @Override
            public boolean matchesSafely(HttpPost item, Description mismatchDescription) {
                try {
                    String actual = retrieveBody(item);
                    mismatchDescription.appendText("Expected body [" + expected + "], but received [" + actual + "]");
                    return expected.equals(actual);
                }
                catch (Exception e) {
                    mismatchDescription.appendText("Encountered exception [" + e + "] attempting match");
                    return false;
                }
            }
        };
    }


    private static String retrieveBody(HttpPost e) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        e.getEntity().writeTo(baos);
        return new String(baos.toByteArray(), "UTF-8");
    }

}
