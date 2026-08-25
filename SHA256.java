import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA256.java
 *
 * Implements SHA-256 (Secure Hash Algorithm 256-bit) hashing.
 *
 * Algorithm Overview:
 *   SHA-256 belongs to the SHA-2 family standardised by NIST (FIPS PUB 180-4).
 *   It produces a fixed 256-bit (32-byte) digest from an arbitrarily long message.
 *
 *   Key properties:
 *     - Deterministic  : Same input always gives the same output.
 *     - Pre-image resistant : Given H(m) it is computationally infeasible to find m.
 *     - Collision resistant : Infeasible to find m1 ≠ m2 with H(m1) = H(m2).
 *     - One-way function  : It CANNOT be reversed / decrypted.
 *
 *   Internal steps (simplified):
 *     1. Pad the message to a multiple of 512 bits.
 *     2. Parse it into 512-bit blocks.
 *     3. Initialise eight 32-bit hash values (H0–H7) from fractional parts of √primes.
 *     4. Process each block through 64 rounds of bit-mixing operations.
 *     5. Concatenate the final H0–H7 to form the 256-bit digest.
 *
 * This class delegates to Java's built-in {@link MessageDigest} which provides
 * a certified, optimised SHA-256 implementation.
 */
public class SHA256 {

    // Algorithm identifier recognised by Java's Security framework
    private static final String ALGORITHM = "SHA-256";

    // -----------------------------------------------------------------------
    // Hashing
    // -----------------------------------------------------------------------

    /**
     * Computes the SHA-256 hash of the supplied text and returns it as a
     * 64-character lowercase hexadecimal string.
     *
     * Example:
     *   hash("hello") → "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
     *
     * @param text  The input string to hash (must not be null or empty).
     * @return      A 64-character hex digest.
     * @throws IllegalArgumentException if text is null or empty.
     * @throws RuntimeException         if SHA-256 is unexpectedly unavailable on the JVM.
     */
    public String hash(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Input text cannot be empty.");
        }

        try {
            // Obtain a MessageDigest instance for SHA-256
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);

            // Convert the text to bytes using UTF-8 and compute the digest
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            // Convert the raw bytes to a hexadecimal string
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java SE specification; this should never happen.
            throw new RuntimeException(
                    "SHA-256 algorithm not available on this JVM. This should never occur.", e);
        }
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Converts a byte array into a lowercase hexadecimal string.
     * Each byte produces exactly two hex characters (zero-padded).
     *
     * @param bytes  Raw byte array (e.g., the output of MessageDigest.digest()).
     * @return       Lowercase hexadecimal string of length bytes.length * 2.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexBuilder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // Mask with 0xFF to treat the byte as unsigned, then format as 2-digit hex
            hexBuilder.append(String.format("%02x", b & 0xFF));
        }
        return hexBuilder.toString();
    }

    // -----------------------------------------------------------------------
    // Utility: compute multiple hashes for display purposes
    // -----------------------------------------------------------------------

    /**
     * Returns a formatted multi-line string showing the hash along with
     * bit-length and byte-length metadata. Useful for UI display.
     *
     * @param text  The text to hash.
     * @return      A formatted result string.
     */
    public String hashWithDetails(String text) {
        String hexHash = hash(text);
        return "SHA-256 Hash (hex)  : " + hexHash + "\n"
                + "Hash length (bits) : " + (hexHash.length() * 4) + "\n"
                + "Hash length (bytes): " + (hexHash.length() / 2) + "\n"
                + "Algorithm          : SHA-256 (FIPS PUB 180-4)";
    }
}
