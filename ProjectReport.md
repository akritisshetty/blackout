# Project Report: Cryptographic Algorithms
## Cryptography & Network Security — Academic Assignment

---

## Table of Contents

1. Introduction
2. RSA Algorithm
3. SHA-256 Hash Function
4. Playfair Cipher
5. Implementation Details
6. Comparative Analysis
7. Conclusion
8. References

---

## 1. Introduction

Cryptography is the science of securing communication by transforming readable data (plaintext) into an unreadable form (ciphertext). This project implements and demonstrates three foundational cryptographic techniques:

- **RSA** — a modern asymmetric (public-key) cipher used in TLS, digital signatures, and secure key exchange.
- **SHA-256** — a cryptographic hash function used for data integrity, digital certificates, and blockchain.
- **Playfair Cipher** — a classical polygraphic substitution cipher, historically significant and pedagogically important.

Each algorithm is implemented in Java, without any external cryptographic libraries (except Java's built-in `MessageDigest` for SHA-256), and exposed through a unified Swing GUI.

---

## 2. RSA Algorithm

### 2.1 History

RSA was published in 1977 by Ron **R**ivest, Adi **S**hamir, and Leonard **A**dleman at MIT. It was the first practical public-key cryptosystem and remains the most widely deployed asymmetric algorithm today.

### 2.2 Mathematical Foundation

RSA's security rests on the **Integer Factorisation Problem**: given a large composite number `n = p × q`, it is computationally infeasible to recover `p` and `q` when both are large primes.

#### Step-by-step Key Generation

| Step | Operation | Description |
|------|-----------|-------------|
| 1 | Choose primes `p`, `q` | Two large, distinct primes (512 bits each in this project) |
| 2 | `n = p × q` | Modulus; public |
| 3 | `φ(n) = (p-1)(q-1)` | Euler's totient function |
| 4 | Choose `e` | `1 < e < φ(n)` and `gcd(e, φ(n)) = 1`; commonly `e = 65537` |
| 5 | `d = e⁻¹ mod φ(n)` | Modular multiplicative inverse (Extended Euclidean Algorithm) |

- **Public Key** = `(e, n)` — shared openly
- **Private Key** = `(d, n)` — kept secret

#### Encryption & Decryption

```
Encrypt : C ≡ Mᵉ (mod n)
Decrypt : M ≡ Cᵈ (mod n)
```

This works because of **Euler's Theorem**:

```
Mᵉᵈ ≡ M (mod n)   when gcd(M, n) = 1
```

Since `ed ≡ 1 (mod φ(n))`, raising M to the power `ed` and reducing mod `n` returns `M`.

### 2.3 Security Considerations

- Key size ≥ 2048 bits is recommended for production systems (this project uses 1024 bits for speed).
- Without padding (PKCS#1v1.5 or OAEP), textbook RSA is vulnerable to chosen-plaintext attacks.
- The private exponent `d` must never be revealed.

### 2.4 Implementation Notes (RSA.java)

- Uses `java.math.BigInteger` for arbitrary-precision arithmetic.
- `BigInteger.probablePrime(512, SecureRandom)` generates cryptographically strong primes.
- `modPow(e, n)` and `modPow(d, n)` perform fast modular exponentiation via square-and-multiply.
- Each byte of plaintext is individually encrypted and stored as hex, then Base64-encoded.

---

## 3. SHA-256 Hash Function

### 3.1 History

SHA-256 (Secure Hash Algorithm, 256-bit) is part of the **SHA-2** family, published by NIST in 2001 (FIPS PUB 180-4). It succeeded the weaker SHA-1 and is now the standard in TLS certificates, Git, Bitcoin, and most modern security protocols.

### 3.2 Mathematical Foundation

SHA-256 is a **Merkle–Damgård** construction operating on 512-bit (64-byte) blocks.

#### Algorithm Steps

**1. Pre-processing (Padding)**

The message is padded to a length that is a multiple of 512 bits:
- Append bit `1`
- Append zeros until length ≡ 448 (mod 512)
- Append the original message length as a 64-bit big-endian integer

**2. Initialise Hash Values**

Eight 32-bit words `H₀ … H₇` initialised to the first 32 bits of the fractional parts of the square roots of the first 8 primes (2, 3, 5, 7, 11, 13, 17, 19):

```
H₀ = 6a09e667
H₁ = bb67ae85
H₂ = 3c6ef372   ... (hex)
```

**3. Round Constants**

64 round constants `K[0..63]` derived from the cube roots of the first 64 primes.

**4. Compression Function (per 512-bit block)**

For each block, 64 rounds of the following operations are applied:

```
S₁  = ROTR²⁶(e) ⊕ ROTR³⁰(e) ⊕ ROTR¹⁵(e)    (Σ₁)
ch  = (e ∧ f) ⊕ (¬e ∧ g)                      (Choice)
T₁  = h + S₁ + ch + K[i] + W[i]
S₀  = ROTR²(a) ⊕ ROTR¹³(a) ⊕ ROTR²²(a)       (Σ₀)
maj = (a ∧ b) ⊕ (a ∧ c) ⊕ (b ∧ c)             (Majority)
T₂  = S₀ + maj
```

where `ROTR^n(x)` = right-rotate x by n bits, `⊕` = XOR, `∧` = AND, `¬` = NOT.

**5. Output**

After processing all blocks, `H₀ … H₇` are concatenated to form the 256-bit digest, displayed as a 64-character hex string.

### 3.3 Properties

| Property | Description |
|----------|-------------|
| **Deterministic** | Same input → always same hash |
| **Pre-image resistant** | Given `H(x)`, cannot find `x` |
| **Second pre-image resistant** | Given `x`, cannot find `y` with `H(x) = H(y)` |
| **Collision resistant** | Cannot find any `x ≠ y` with `H(x) = H(y)` |
| **Avalanche effect** | 1-bit change in input flips ~50% of output bits |

### 3.4 Use Cases

- **Password storage** (with salt): databases store `H(salt ‖ password)`
- **Digital signatures**: sign `H(message)` instead of the full message
- **Data integrity**: file checksums
- **Blockchain**: Bitcoin's proof-of-work requires `H(block)` to start with N zero bits

### 3.5 Implementation Notes (SHA256.java)

- Delegates to `java.security.MessageDigest.getInstance("SHA-256")`.
- The JCA (Java Cryptography Architecture) provides a FIPS-compliant, hardware-accelerated implementation.
- The Decrypt button is disabled in the UI when SHA-256 is selected.

---

## 4. Playfair Cipher

### 4.1 History

Invented by Sir Charles **Wheatstone** in 1854, the cipher was promoted by Lord **Playfair** (hence the name). It was the first practical digraphic substitution cipher and was used by the British Army in the First World War.

### 4.2 Mathematical Foundation

Playfair operates on **pairs of letters** (digraphs) rather than individual characters, making simple frequency analysis significantly harder than with monoalphabetic ciphers.

#### 4.2.1 Key Square Construction

1. Write the keyword (unique letters only, I=J).
2. Fill the remaining 24 letters of the alphabet (excluding J) in order.
3. Arrange in a 5×5 grid.

Example — keyword **"MONARCHY"**:

```
M  O  N  A  R
C  H  Y  B  D
E  F  G  I  K
L  P  Q  S  T
U  V  W  X  Z
```

#### 4.2.2 Text Preparation

1. Convert to uppercase; replace J → I.
2. Split into digraphs.
3. If both letters in a pair are identical, insert **X** between them.
4. If the total length is odd, append **X**.

Example: `BALLOON` → `BA LX LO ON`

#### 4.2.3 Encryption Rules

For each digraph `(A, B)` locate their positions `(r₁,c₁)` and `(r₂,c₂)`:

| Condition | Rule |
|-----------|------|
| Same row (`r₁ = r₂`) | Each letter → letter to its **right** (wrap) |
| Same column (`c₁ = c₂`) | Each letter → letter **below** it (wrap) |
| Rectangle | A → `(r₁, c₂)`, B → `(r₂, c₁)` (swap columns) |

#### 4.2.4 Decryption Rules

Identical to encryption except:
- Same row → shift **left**
- Same column → shift **up**
- Rectangle → same swap (self-inverse)

### 4.3 Strengths and Weaknesses

| Strengths | Weaknesses |
|-----------|------------|
| 25²×24! possible key squares | Only 25 distinct cipher letters |
| Digraphic; resists simple frequency analysis | 600 digraph frequencies still analysable |
| Easy to use manually | Vulnerable to known-plaintext attack |
| Historical security over monoalphabetic | Not suitable for modern secure communication |

### 4.4 Implementation Notes (Playfair.java)

- `LinkedHashSet` preserves insertion order during key square construction.
- The `prepareText()` method handles all edge cases: duplicate letters, odd length, non-letter characters.
- `findPosition()` performs a linear scan of the 5×5 matrix for clarity.
- Output digraphs are space-separated for readability.

---

## 5. Implementation Details

### 5.1 Project Architecture

```
┌─────────────────────────────────────────┐
│              MainUI.java                │
│  (Swing GUI — JFrame, JTextArea, etc.)  │
└────────┬──────────┬──────────┬──────────┘
         │          │          │
    ┌────▼────┐ ┌───▼───┐ ┌───▼──────┐
    │ RSA.java│ │SHA256 │ │Playfair  │
    │         │ │.java  │ │.java     │
    └─────────┘ └───────┘ └──────────┘
```

Each algorithm class is self-contained. `MainUI` only calls their public methods:

```java
rsa.encrypt(text)       rsa.decrypt(cipher)
sha256.hash(text)       sha256.hashWithDetails(text)
playfair.encrypt(text, key)    playfair.decrypt(cipher, key)
playfair.getMatrixDisplay(key)
rsa.getPublicKey()      rsa.getPrivateKey()
```

### 5.2 Error Handling Strategy

- Algorithm classes throw `IllegalArgumentException` with descriptive messages.
- `MainUI` catches all exceptions and displays them via `JOptionPane.showMessageDialog`.
- A colour-coded status bar provides instant feedback (green/yellow/red).

### 5.3 Technology Choices

| Choice | Reason |
|--------|--------|
| `BigInteger` for RSA | Handles arbitrarily large integers natively; no overflow |
| `SecureRandom` for prime generation | Cryptographic-quality randomness |
| `MessageDigest` for SHA-256 | JCA-certified, hardware-accelerated, platform standard |
| `LinkedHashSet` in Playfair | O(1) membership test + insertion-order iteration |
| Java Swing | No external dependencies; runs on any JRE |

---

## 6. Comparative Analysis

| Feature | RSA | SHA-256 | Playfair |
|---------|-----|---------|----------|
| Type | Asymmetric cipher | Hash function | Symmetric cipher |
| Era | 1977 (modern) | 2001 (modern) | 1854 (classical) |
| Reversible? | Yes | No | Yes |
| Key | Public + Private | None | Shared keyword |
| Security basis | Integer factorisation | Bitwise diffusion/confusion | Letter-pair transposition |
| Output size | Variable | Fixed (256 bits) | Same length as input |
| Modern use? | Yes (HTTPS, SSH) | Yes (TLS, Git, Bitcoin) | Educational only |

---

## 7. Conclusion

This project demonstrates the evolution of cryptographic thinking across three centuries:

- **Playfair** (1854) shows how simple grid-based digraphic substitution was a significant advance over Caesar/Vigenère ciphers, yet is still vulnerable to statistical analysis.
- **RSA** (1977) introduced the revolutionary concept of public-key cryptography, solving the key-distribution problem and enabling secure communication over untrusted channels.
- **SHA-256** (2001) represents the modern need for data integrity verification — a one-way function that is infeasible to reverse or to find collisions for.

Together, they illustrate the shift from security through obscurity to mathematically provable computational hardness, which is the foundation of all modern cryptographic systems.

---

## 8. References

1. Rivest, R. L., Shamir, A., & Adleman, L. (1978). *A method for obtaining digital signatures and public-key cryptosystems.* Communications of the ACM, 21(2), 120–126.
2. NIST. (2015). *FIPS PUB 180-4: Secure Hash Standard (SHS).* National Institute of Standards and Technology.
3. Stallings, W. (2017). *Cryptography and Network Security: Principles and Practice* (7th ed.). Pearson.
4. Menezes, A., van Oorschot, P., & Vanstone, S. (1996). *Handbook of Applied Cryptography.* CRC Press. (Available free at cacr.uwaterloo.ca/hac)
5. Singh, S. (1999). *The Code Book.* Doubleday. (Playfair history, Chapter 2)
6. Oracle. *Java SE 17 API — java.math.BigInteger.* https://docs.oracle.com/en/java/docs/api/java.base/java/math/BigInteger.html
7. Oracle. *Java SE 17 API — java.security.MessageDigest.* https://docs.oracle.com/en/java/docs/api/java.base/java/security/MessageDigest.html
