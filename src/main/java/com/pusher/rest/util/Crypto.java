package com.pusher.rest.util;

import com.google.gson.Gson;
import com.pusher.rest.data.EncryptedPayload;
import org.abstractj.kalium.crypto.Random;
import org.abstractj.kalium.crypto.SecretBox;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Crypto is a class containing all the utility methods needed to do End to End Encryption for Encrypted Channels.
 * Read more here: https://pusher.com/docs/client_api_guide/client_encrypted_channels
 */
public class Crypto {
    /**
     * The prefix that is used to identify End to End Encrypted Channels
     */
    private static final String privateEncryptedChannelPrefix = "private-encrypted-";

    /**
     * The GSON instance to be used for serialisation
     */
    private final Gson bodySerialiser;

    /**
     * 32 character long secret you define for end to end encryption
     */
    private final String encryptionMasterKey;

    /** Construct an instance of the Crypto object which will allow for encryption of Pusher Channels payloads.
     * @param encryptionMasterKey The Encryption Master Key, a 32 character long secret you define for end to end encryption
     * @param serialiser The GSON instance for serialisation of the payload
     * @throws RuntimeException if Libsodium isn't available on the platform
     */
    public Crypto(String encryptionMasterKey, Gson serialiser) throws RuntimeException {
        this.bodySerialiser = serialiser;
        this.encryptionMasterKey = encryptionMasterKey;
        if (!cryptoAvailable()) {
            throw new RuntimeException("The Pusher client requires Libsodium for End to End Encryption");
        }
    }

    /**
     * Encrypt a given payload on a given channel using SecretBox from NaCl.
     * @param channelName The name of the channel the payload will be triggered on.
     * @param data A serialisable object containing the plaintext data.
     * @return EncryptedPayload object containing a nonce and the ciphertext.
     */
    public EncryptedPayload encrypt(String channelName, Object data) {
        if (!isEncryptedChannel(channelName)){
            throw new IllegalArgumentException("Tried to encrypt an event for a channel lacking the `private-encrypted-` prefix");
        }
        byte[] sharedSecret = generateSharedSecret(channelName);
        byte[] nonce = generateNonce();
        String nonceB64 = Base64.getEncoder().encodeToString(nonce);
        String serialisedData = bodySerialiser.toJson(data);
        SecretBox s = new SecretBox(sharedSecret);
        byte[] cipherText = s.encrypt(nonce, serialisedData.getBytes());
        String CipherTextB64 = Base64.getEncoder().encodeToString(cipherText);
        return new EncryptedPayload(nonceB64, CipherTextB64);
    }

    /**
     * Generates a shared secret by doing a sha256 sum of the channelName concatenated with the master key.
     * @param channelName the channel
     * @return byte[] containing shared secret.
     */
    public byte[] generateSharedSecret(String channelName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((channelName+ encryptionMasterKey).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Your platform does not support SHA-256, which is required for end to end encryption");
        }
    }

    /**
     * Checks whether the channel is an End to End Encrypted Channel.
     * @param channelName the name of the channel to check
     * @return Boolean true if it is, false if not.
     */
    public static boolean isEncryptedChannel(String channelName) {
        return channelName.startsWith(privateEncryptedChannelPrefix);
    }

    /**
     * Checks whether the underlying platform supports End to End Encryption (ie, has the libSodium dependency).
     * @return Boolean true if it does, false if not.
     */
    public static boolean cryptoAvailable() {
        try {
            generateNonce();
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    private static byte[] generateNonce() throws UnsatisfiedLinkError {
        Random r = new Random();
        return r.randomBytes(24);
    }
}
