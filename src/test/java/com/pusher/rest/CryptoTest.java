package com.pusher.rest;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pusher.rest.util.Crypto;
import com.pusher.rest.util.EncryptedPayload;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class CryptoTest {
    private static final Gson BODY_SERIALISER = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    private final static char[] hexArray = "0123456789abcdef".toCharArray();
    @Test
    public void testGenerateSharedSecret() {

        assumeTrue(Crypto.cryptoAvailable());
        Crypto pc = new Crypto("This is a string that is 32 chars", BODY_SERIALISER);
        byte[] sharedSecret = pc.generateSharedSecret("private-encrypted-bla");
        String sharedSecretB64 = bytesToHex(sharedSecret);
        String expected = "004831f99d2a4e86723e893caded3a2897deeddbed9514fe9497dcddc52bd50b";
        Assert.assertEquals(expected, sharedSecretB64);
    }
    @Test
    public void testEncrypt() {
        assumeTrue( Crypto.cryptoAvailable());
        String channelName = "private-encrypted-bla";
        String data = "Hello! Hello! Hello!";
        String encryptionKey = "This is a string that is 32 chars";
        Crypto pc = new Crypto(encryptionKey, BODY_SERIALISER);
        EncryptedPayload ep = pc.encrypt(channelName, data);
        Assert.assertNotNull(ep.getCiphertext());
        Assert.assertNotNull(ep.getNonce());
    }
/* Helper method to encode bytes into hex, useful for testing that shared secret generator is making secrets correctly. */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}