package com.blackout.crypto;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * BLACKOUT // AsymmetricEngine
 *
 * RSA-2048 "operative badge" machinery built on the stock JCA provider
 * ({@link java.security} + {@link javax.crypto}) - no external libraries.
 *
 * Design notes:
 *   - Padding: RSA-OAEP with SHA-256 for both the OAEP digest and MGF1. The parameters
 *     are passed EXPLICITLY because the JCA convenience name "OAEPWithSHA-256AndMGF1Padding"
 *     silently defaults MGF1 to SHA-1 on the stock provider, which no browser's WebCrypto
 *     will accept. The explicit spec is wire-compatible with WebCrypto RSA-OAEP + SHA-256.
 *     OAEP is randomized, so the same keyword encrypts to a different blob every
 *     transmission (defeats replay/matching).
 *   - Keys travel as Base64(X.509) / Base64(PKCS#8) strings so they can be pasted into
 *     dossiers, stored in badges and shipped over JSON.
 *   - OAEP-SHA256 caps plaintext at 190 bytes for a 2048-bit modulus - ample for a
 *     Playfair keyword, and deliberately guarded below.
 *
 * Thread-safety: stateless static utility, safe for concurrent use.
 */
public final class AsymmetricEngine {

    public static final int KEY_SIZE_BITS = 2048;

    private static final String ALGORITHM = "RSA";
    /**
     * OAEP(SHA-256, MGF1-SHA-256, empty label) - identical to WebCrypto's
     * { name: 'RSA-OAEP', hash: 'SHA-256' }. Set explicitly; see class javadoc.
     */
    private static final OAEPParameterSpec OAEP_SPEC =
            new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    /** 256-byte modulus minus two SHA-256 hashes minus 2 octets of OAEP overhead. */
    private static final int MAX_PLAINTEXT_BYTES = 190;

    private AsymmetricEngine() {
        // static utility - never instantiated
    }

    /* ------------------------------------------------------------------
     * Badge (key pair) lifecycle
     * ------------------------------------------------------------------ */

    /** Mints a fresh 2048-bit RSA key pair ("operative badge"). */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(KEY_SIZE_BITS);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new CryptoOperationException("Unable to mint RSA-2048 operative badge", e);
        }
    }

    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String encodePrivateKey(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /** Parses a Base64 X.509 public key back into a usable object. */
    public static PublicKey decodePublicKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(base64.trim());
            return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new CryptoOperationException("Malformed public key - expected Base64 X.509 encoding", e);
        }
    }

    /** Parses a Base64 PKCS#8 private key back into a usable object. */
    public static PrivateKey decodePrivateKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(base64.trim());
            return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new CryptoOperationException("Malformed private key - expected Base64 PKCS#8 encoding", e);
        }
    }

    /* ------------------------------------------------------------------
     * Key transport
     * ------------------------------------------------------------------ */

    /**
     * Seals a short secret (the Playfair keyword) under the recipient's public key.
     * Output is Base64 ciphertext.
     */
    public static String encrypt(String plaintext, PublicKey recipientPublicKey) {
        byte[] data = plaintext.getBytes(StandardCharsets.UTF_8);
        if (data.length > MAX_PLAINTEXT_BYTES) {
            throw new CryptoOperationException(
                    "Secret exceeds RSA-OAEP capacity (" + MAX_PLAINTEXT_BYTES + " bytes); use a shorter keyword.");
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey, OAEP_SPEC);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data));
        } catch (Exception e) {
            throw new CryptoOperationException("RSA encryption failed", e);
        }
    }

    /**
     * Unseals a keyword with the recipient's private key. A wrong or corrupt input
     * raises {@link CryptoOperationException} rather than returning garbage.
     */
    public static String decrypt(String base64Ciphertext, PrivateKey ownerPrivateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.DECRYPT_MODE, ownerPrivateKey, OAEP_SPEC);
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(base64Ciphertext.trim()));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoOperationException("RSA decryption failed - wrong badge or corrupted key blob", e);
        }
    }
}
