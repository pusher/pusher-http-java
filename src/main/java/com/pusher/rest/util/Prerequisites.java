package com.pusher.rest.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Prerequisites {

    public static void nonNull(final String name, final Object ref) {
        if (ref == null) throw new IllegalArgumentException("Parameter [" + name + "] must not be null");
    }

    public static void nonEmpty(final String name, final String ref) {
        nonNull(name, ref);
        if (ref.length() == 0) throw new IllegalArgumentException("Parameter [" + name + "] must not be empty");
    }

    public static void maxLength(final String name, final int max, final List<?> ref) {
        if (ref.size() > max) throw new IllegalArgumentException("Parameter [" + name + "] must have size < " + max);
    }

    public static void noNullMembers(final String name, final List<?> ref) {
        for (Object e : ref) {
            if (e == null) throw new IllegalArgumentException("Parameter [" + name + "] must not contain null elements");
        }
    }

    public static void isValidSha256Key(final String name, final String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(), "SHA256"));
            // If that goes OK, then we're good to go
        }
        catch (final NoSuchAlgorithmException e) {
            // Out of luck.
            throw new RuntimeException("The Pusher REST client requires HmacSHA256 support", e);
        }
        catch (final InvalidKeyException e) {
            // Failed the test
            throw new IllegalArgumentException("Parameter [" + name + "] must be a valid SHA256 key", e);
        }
    }
}
