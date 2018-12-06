package com.pusher.rest;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pusher.rest.util.Crypto;
import com.pusher.rest.util.EncryptedPayload;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Base64;

import static org.junit.Assume.assumeTrue;

public class CryptoTest {
    private static final Gson BODY_SERIALISER = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private Crypto pc = null;
    @Before
    public void initialize() {
        if(Crypto.cryptoAvailable()) {
             pc = new Crypto("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", BODY_SERIALISER);
        }
    }
    @Test
    public void testGenerateSharedSecret() {
        assumeTrue(Crypto.cryptoAvailable());
        // Check that the secret generation is generating consistent secrets
        byte[] sharedSecret = pc.generateSharedSecret("private-encrypted-channel-a");
        String sharedSecretB64 = Base64.getEncoder().encodeToString(sharedSecret);
        String expected = "Rp+wpkNpL89qhqco1JkIG31AVXyU8PUVJBr1B2MvdoA=";
        Assert.assertEquals(expected, sharedSecretB64);

        // Check that the secret generation is using the channel as a part of the generation
        byte[] sharedSecret2 = pc.generateSharedSecret("private-encrypted-channel-b");
        if(sharedSecret == sharedSecret2) {
            Assert.fail();
        }

        // Check that the secret generation is using the encryption master key as a part of the generation
        Crypto pc2 = new Crypto("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", BODY_SERIALISER);
        byte[] sharedSecret3 = pc2.generateSharedSecret("private-encrypted-channel-a");


        String sharedSecret3B64 = Base64.getEncoder().encodeToString(sharedSecret3);
        if(sharedSecretB64.equals(sharedSecret3B64)) {
            Assert.fail();
        }
    }

    @Test
    public void testIsEncryptedChannel() {
        assumeTrue(Crypto.cryptoAvailable());
        Assert.assertTrue(Crypto.isEncryptedChannel("private-encrypted-test"));
        Assert.assertFalse(Crypto.isEncryptedChannel("private-encrypted"));
        Assert.assertFalse(Crypto.isEncryptedChannel("test-private-encrypted"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncryptPayloadNoChannel() {
        assumeTrue(Crypto.cryptoAvailable());
        String channel = "";
        String payload = "now that's what I call a payload!";
        pc.encrypt(channel, payload);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncryptPayloadPublicChannel() {
        assumeTrue(Crypto.cryptoAvailable());
        String channel = "public-static-void-main";
        String payload = "now that's what I call a payload!";
        pc.encrypt(channel, payload);
    }

    @Test
    public void testEncrypt() {
        assumeTrue(Crypto.cryptoAvailable());
        String channelName = "private-encrypted-bla";
        String data = "Hello! Hello! Hello!";
        String encryptionKey = "This is a string that is 32 chars";
        Crypto pc = new Crypto(encryptionKey, BODY_SERIALISER);
        EncryptedPayload ep = pc.encrypt(channelName, data);
        Assert.assertNotNull(ep.getCiphertext());
        Assert.assertNotNull(ep.getNonce());
    }

}
