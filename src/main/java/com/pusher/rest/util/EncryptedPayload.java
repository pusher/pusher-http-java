package com.pusher.rest.util;

public class EncryptedPayload {

    private final String nonce;
    private final String ciphertext;

    public EncryptedPayload(String nonce, String ciphertext) {
        this.nonce = nonce;
        this.ciphertext = ciphertext;
    }

    public String getNonce() {
        return nonce;
    }

    public String getCiphertext() {
        return ciphertext;
    }
}
