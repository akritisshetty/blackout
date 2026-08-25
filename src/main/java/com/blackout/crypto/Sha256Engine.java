package com.blackout.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * BLACKOUT // Sha256Engine
 *
 * Tamper-evident "digital seal" machinery. Wraps {@link java.security.MessageDigest}
 * and renders digests as lowercase hex. Every dead drop buried through the backend is
 * sealed with the digest of its canonical package string; every retrieval re-computes
 * that digest and compares it against the stored seal.
 */
public final class Sha256Engine {

    private static final String ALGORITHM = "SHA-256";

    private Sha256Engine() {
        // static utility - never instantiated
    }

    /**
     * Digests the input (UTF-8) and returns 64 lowercase hex characters.
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java platform spec; this is unreachable in practice.
            throw new CryptoOperationException("JVM runtime is missing SHA-256 support", e);
        }
    }
}
