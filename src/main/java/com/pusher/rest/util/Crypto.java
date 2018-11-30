package com.pusher.rest.util;

import com.google.gson.Gson;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Crypto {
    private static final String PRIVATE_ENCRYPTED_CHANNEL_PREFIX = "private-encrypted-";

    private final Gson BODY_SERIALISER;
    private final String ENCRYPTION_KEY;

    public Crypto(String encryptionKey, Gson serialiser) throws RuntimeException {
        this.BODY_SERIALISER = serialiser;
        this.ENCRYPTION_KEY = encryptionKey;
        if(!cryptoAvailable()) {
            throw new RuntimeException("The Pusher client requires Libsodium for End to End Encryption");
        }
    }

    public EncryptedPayload encrypt(String channelName, Object data) {
        if(!isEncryptedChannel(channelName)){
            throw new IllegalArgumentException("Tried to encrypt an event for a channel lacking the `private-encrypted-` prefix");
        }
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
            throw new RuntimeException("Your platform does not support SHA-256, which is required for end to end encryption");
        }
    }
    public static boolean isEncryptedChannel(String channel) {
        return channel.startsWith(PRIVATE_ENCRYPTED_CHANNEL_PREFIX);
    }
    public static boolean cryptoAvailable() {
        try {
            generateNonce();
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
    public static byte[] generateNonce() throws UnsatisfiedLinkError {
        Random r = new Random();
        return r.randomBytes(24);
    }
}
