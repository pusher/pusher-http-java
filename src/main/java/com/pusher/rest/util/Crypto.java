package com.pusher.rest.util;

import com.google.gson.Gson;
import org.abstractj.kalium.NaCl;
import org.abstractj.kalium.crypto.*;
import org.abstractj.kalium.encoders.Encoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Crypto {
    private static final String PRIVATE_ENCRYPTED_CHANNEL_PREFIX = "private-encrypted-";

    private final Gson BODY_SERIALISER;
    private final String ENCRYPTION_KEY;

    public Crypto(String encryptionKey, Gson serialiser) throws UnsupportedOperationException {
        this.BODY_SERIALISER = serialiser;
        this.ENCRYPTION_KEY = encryptionKey;
    }

    public byte[] generateNonce() {
        Random r = new Random();
        return r.randomBytes(24);
    }

    public EncryptedPayload encrypt(String channelName, Object data) {
        byte[] sharedSecret = generateSharedSecret(channelName);
        byte[] nonce = generateNonce();
        String nonceB64 = Base64.getEncoder().encodeToString(nonce);
        String serialisedData = BODY_SERIALISER.toJson(data);
        SecretBox s = new SecretBox(sharedSecret);
        byte[] cipherText = s.encrypt(nonce, serialisedData.getBytes());
        String CipherTextB64 = Base64.getEncoder().encodeToString(cipherText);
        return new EncryptedPayload(nonceB64, CipherTextB64);
    }

    public byte[] generateSharedSecret(String channelName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((channelName+ENCRYPTION_KEY).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Your platform does not support SHA-256, which is required for end to end encryption.");
        }
    }
    public static boolean isEncryptedChannel(String channel) {
        return channel.startsWith(PRIVATE_ENCRYPTED_CHANNEL_PREFIX);
    }
}
