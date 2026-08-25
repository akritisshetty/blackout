package com.blackout.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * BLACKOUT // DeadDropProtocol
 *
 * The canonical wire contract shared by the backend sealer and the frontend verifier.
 * Both sides agree, forever, on:
 *
 *   canonical = encryptedPayload + '|' + encryptedKey
 *   seal      = SHA-256(canonical)                 (computed by backend, stored immutable)
 *   verified  = constantTimeEquals(SHA-256(canonical), seal)   (computed by client on fetch)
 *
 * The payload and key are machine-generated (hex ciphertext / Base64 blobs), so a '|'
 * collision inside them is impossible in practice; the delimiter simply removes any
 * ambiguity when concatenating two variable-length fields.
 */
public final class DeadDropProtocol {

    public static final char FIELD_DELIMITER = '|';

    private DeadDropProtocol() {
        // static utility - never instantiated
    }

    /** Canonical package string that the seal is computed over. */
    public static String canonicalPackage(String encryptedPayload, String encryptedKey) {
        return encryptedPayload + FIELD_DELIMITER + encryptedKey;
    }

    /** Computes the SHA-256 digital seal for a package. Used by the backend at burial time. */
    public static String computeSeal(String encryptedPayload, String encryptedKey) {
        return Sha256Engine.sha256Hex(canonicalPackage(encryptedPayload, encryptedKey));
    }

    /**
     * Recomputes the seal for a fetched package and compares it to the stored seal using
     * {@link MessageDigest#isEqual} - a constant-time comparison that does not leak
     * match position through early exit.
     *
     * @return true when the package is byte-for-byte identical to what was buried
     */
    public static boolean verifySeal(String encryptedPayload, String encryptedKey, String expectedSeal) {
        String actual = computeSeal(encryptedPayload, encryptedKey);
        String expected = expectedSeal == null ? "" : expectedSeal.trim().toLowerCase();
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
