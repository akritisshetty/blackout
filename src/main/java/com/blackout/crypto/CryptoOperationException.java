package com.blackout.crypto;

/**
 * BLACKOUT // CryptoOperationException
 *
 * Unchecked domain exception for every cryptographic engine. Keeps the engines free of
 * checked-exception clutter and lets UI/REST layers handle failures at a single choke
 * point with a human-readable field-report message.
 */
public class CryptoOperationException extends RuntimeException {

    public CryptoOperationException(String message) {
        super(message);
    }

    public CryptoOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
