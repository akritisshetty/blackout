# Cryptography & Network Security — Java Application

A desktop application demonstrating **RSA**, **SHA-256**, and the **Playfair Cipher** via a dark-themed Java Swing GUI.

---

## Project Structure

```
CryptographyProject/
├── MainUI.java       — Swing GUI + entry point
├── RSA.java          — RSA key generation, encryption, decryption
├── SHA256.java       — SHA-256 hashing
├── Playfair.java     — Playfair cipher encryption & decryption
├── README.md         — This file
└── ProjectReport.md  — Concepts & mathematical background
```

---

## Requirements

- Java 8 or later (JDK)
- No external libraries needed

---

## How to Compile & Run

Open a terminal inside the `CryptographyProject/` folder and run:

```bash
# Compile all source files
javac *.java

# Run the application
java MainUI
```

---

## Features

| Feature | Details |
|---|---|
| RSA Encrypt / Decrypt | Auto-generates 1024-bit key pair on startup |
| SHA-256 Hash | One-way; Decrypt button disabled automatically |
| Playfair Encrypt / Decrypt | Enter a keyword; matrix display available |
| Show RSA Keys | Popup showing public & private key (e, d, n) |
| Show Playfair Matrix | 5×5 grid built from the entered keyword |
| Copy Output | Copies output area to clipboard |
| Clear All | Resets both text areas and the key field |
| Status Bar | Colour-coded messages (green = ok, yellow = info, red = error) |
| Dark Theme | Full dark UI, no extra dependencies |

---

## Usage Guide

### RSA
1. Select **RSA** from the dropdown.
2. Type or paste any text in the **Input** area.
3. Click **Encrypt** → ciphertext appears in **Output**.
4. Click **Decrypt** → original text restored in **Input** area.
5. Click **Show RSA Keys** to inspect the generated key pair.

### SHA-256
1. Select **SHA-256**.
2. Enter text and click **Encrypt** (= Hash).
3. The 64-character hex digest appears in **Output**.
4. **Decrypt** is disabled — SHA-256 is irreversible by design.

### Playfair Cipher
1. Select **Playfair Cipher**.
2. Enter a keyword in the **Keyword** field.
3. Type plaintext in **Input** and click **Encrypt**.
4. Click **Show Matrix** to see the 5×5 key square.
5. Click **Decrypt** to reverse (Output → Input).

---

## Notes

- RSA operates on individual UTF-8 bytes for simplicity. For production use, employ PKCS#1 or OAEP padding.
- SHA-256 uses Java's built-in `java.security.MessageDigest`.
- Playfair treats **I** and **J** as the same letter (standard rule).
- Filler character **X** may appear in Playfair decrypted output.
