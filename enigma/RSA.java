import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/*
RSA.java

- Implements RSA public-key cryptography using Java's BigInteger.

* Algorithm Overview:
1. Choose two large primes p and q.
2. Compute n = p * q  (the modulus).
3. Compute phi(n) = (p-1)*(q-1).
4. Choose e such that 1 < e < phi(n) and gcd(e, phi(n)) = 1.
5. Compute d = e^(-1) mod phi(n)  (modular inverse).
- Public key  : (e, n)
- Private key : (d, n)

- Encrypt : C = M^e mod n
- Decrypt : M = C^d mod n
 
- Each character of the plaintext is encrypted individually to keep the implementation simple and easy to understand.
*/
 
public class RSA {

    private BigInteger p;       // First prime
    private BigInteger q;       // Second prime
    private BigInteger n;       // Modulus  n = p * q
    private BigInteger phi;     // Euler's totient  phi(n) = (p-1)*(q-1)
    private BigInteger e;       // Public exponent
    private BigInteger d;       // Private exponent

    // Bit length used when generating primes (512 bits per prime - 1024-bit key)
    private static final int KEY_BITS = 512;

    // Common public exponent value (65537 is widely used in practice)
    private static final BigInteger E_VALUE = BigInteger.valueOf(65537);

    // Random source with cryptographic-quality entropy
    private static final SecureRandom RANDOM = new SecureRandom();

    // Constructor – key generation happens here
    /*
     - Generates a fresh RSA key pair when instantiated.
     - Call getPublicKey() and getPrivateKey() to retrieve human-readable
     - representations after construction.
    */
    
    public RSA() {
        generateKeys();
    }

    // Key Generation
    /*
     - Generates RSA keys:
        - Picks two random primes p and q of KEY_BITS each.
        - Derives n, phi(n), e, and d.
        - Repeats if the chosen e does not satisfy gcd(e, phi) == 1.
    */
    
    private void generateKeys() {
        // Step 1 : Generate two distinct primes p and q
        p = BigInteger.probablePrime(KEY_BITS, RANDOM);
        do {
            q = BigInteger.probablePrime(KEY_BITS, RANDOM);
        } while (q.equals(p));  // Ensure p ≠ q

        // Step 2 : Compute modulus
        n = p.multiply(q);

        // Step 3 : Compute Euler's totient
        phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        // Step 4 : Choose public exponent e
        // We prefer 65537; fall back to a search if gcd(e, phi) ≠ 1
        if (E_VALUE.compareTo(phi) < 0 && phi.gcd(E_VALUE).equals(BigInteger.ONE)) {
            e = E_VALUE;
        } else {
            e = BigInteger.valueOf(3);
            while (!phi.gcd(e).equals(BigInteger.ONE)) {
                e = e.add(BigInteger.TWO);
            }
        }

        // Step 5 : Compute private exponent d = e^(-1) mod phi(n)
        d = e.modInverse(phi);
    }

    // Encryption
    /*
     - Encrypts the given plaintext using the RSA public key (e, n).
     
     - Each byte of the UTF-8 encoded plaintext is encrypted separately and the resulting BigInteger values are joined with spaces, then Base64 encoded for safe transport.
     
      @param plaintext  The message to encrypt.
      @return           A Base64-encoded string of space-delimited ciphertext blocks.
      @throws 		IllegalArgumentException if plaintext is null or empty.
    */
    
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be empty.");
        }

        byte[] bytes = plaintext.getBytes(StandardCharsets.UTF_8);
        StringBuilder encryptedBlocks = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            // Convert byte to unsigned BigInteger and encrypt: C = M^e mod n
            BigInteger messageByte = BigInteger.valueOf(bytes[i] & 0xFF);
            BigInteger cipherBlock = messageByte.modPow(e, n);
            encryptedBlocks.append(cipherBlock.toString(16)); // hex representation
            if (i < bytes.length - 1) {
                encryptedBlocks.append(" ");
            }
        }

        // Encode the final string in Base64 for clean display
        return Base64.getEncoder().encodeToString(
                encryptedBlocks.toString().getBytes(StandardCharsets.UTF_8));
    }

    // Decryption
    /*
     - Decrypts a ciphertext produced by {@link #encrypt(String)}.
     
     - Reverses the Base64 encoding, splits on spaces to recover hex blocks, applies M = C^d mod n to each block, and reconstructs the original string.
     
      @param ciphertext  The Base64-encoded ciphertext returned by encrypt().
      @return            The original plaintext message.
      @throws IllegalArgumentException if ciphertext is null, empty, or malformed.
    */
    
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            throw new IllegalArgumentException("Ciphertext cannot be empty.");
        }

        try {
            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(ciphertext.trim());
            String blockString = new String(decoded, StandardCharsets.UTF_8);
            String[] hexBlocks = blockString.split(" ");

            byte[] decryptedBytes = new byte[hexBlocks.length];

            for (int i = 0; i < hexBlocks.length; i++) {
                BigInteger cipherBlock = new BigInteger(hexBlocks[i].trim(), 16);
                // Decrypt: M = C^d mod n
                BigInteger messageByte = cipherBlock.modPow(d, n);
                decryptedBytes[i] = messageByte.byteValueExact();
            }

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Ciphertext is malformed. Please use the ciphertext generated by this application.");
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "Decryption failed. The ciphertext may have been altered or does not belong to the current key pair.");
        }
    }

    // Accessors for key display
    
    /* Returns a summary of the public key (e, n). */
    public String getPublicKey() {
        return "Public Key (e, n):\n"
                + "  e = " + e.toString() + "\n"
                + "  n = " + n.toString();
    }

    /* Returns a summary of the private key (d, n). */
    public String getPrivateKey() {
        return "Private Key (d, n):\n"
                + "  d = " + d.toString() + "\n"
                + "  n = " + n.toString();
    }

    /* Returns the modulus n (as a decimal string). */
    public String getModulus() {
        return n.toString();
    }

    /** Returns the public exponent e. */
    public BigInteger getE() { return e; }

    /** Returns the private exponent d. */
    public BigInteger getD() { return d; }

    /** Returns the modulus n as a BigInteger. */
    public BigInteger getN() { return n; }
}
