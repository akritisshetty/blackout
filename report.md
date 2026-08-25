# BLACKOUT - The Cipher Game
*"a simple full stack Java web game that teaches cryptography by making you use it"*

## 1. Introduction

**BLACKOUT** is an educational, cryptography-themed web game built as a full-stack Java application. Players take on the role of a spy ("agent") and complete espionage-themed missions that teach real cryptographic algorithms — Playfair cipher, RSA-2048, and SHA-256 — by making them use it. The game is designed to be both a fun puzzle experience and a hands-on learning tool for classical and modern cryptography.


---

## 2. Objectives

- Teach players how classical and modern cryptographic algorithms work through interactive gameplay.
- Demonstrate real symmetric cryptography (Playfair Cipher) and asymmetric cryptography (RSA-2048 OAEP) operating between a Java backend and browser-side WebCrypto.
- Provide a tamper-evident system (SHA-256 sealed packages) to illustrate hash-based integrity verification.

---

## 3. Architecture

### 3.1 Backend — Layered Monolith

The backend follows a standard Spring Boot layered architecture with in-memory storage (no database):

```
┌─────────────────────────────────────────────┐
│           Controller Layer (REST)            │
│  AgentController | MissionController |       │
│  ToolController                           │
├─────────────────────────────────────────────┤
│             Service Layer                   │
│  AgentService | MissionService |             │
├─────────────────────────────────────────────┤
│            Crypto Layer                     │
│  PlayfairEngine | AsymmetricEngine |         │
│  Sha256Engine   | DeadDropProtocol           │
├─────────────────────────────────────────────┤
│         Storage Layer                       │
│  AgentStore (ConcurrentHashMap)              │
│  MissionSessionStore (ConcurrentHashMap)     │
├─────────────────────────────────────────────┤
│              Game Layer                     │
│  MissionType · MissionBank · PendingMission  │
└─────────────────────────────────────────────┘
```

### 3.2 Frontend — Single-Page Application

The frontend is a vanilla JavaScript SPA served from Spring Boot's `static/` directory with no build step or framework:

- **`index.html`** — Single page with nav tabs (PLAY / TOP AGENTS), play area, status bar, toast notifications.
- **`js/api.js`** — Fetch wrapper for all REST endpoints (IIFE exposing `window.API`).
- **`js/ui.js`** — DOM helper utilities (`el()`, `$()`, `toast()`, `setStatus()`).
- **`js/badge.js`** — WebCrypto RSA-2048 key pair generation and localStorage persistence.
- **`js/game.js`** — Complete game loop: session management, mission rendering for all 4 types, scoring, leaderboard.
- **`css/blackout.css`** — Dark tactical theme with neon green accents and monospace font (~475 lines).

### 3.3 Deployment Architecture

A single Spring Boot fat JAR serves both the REST API and static frontend. All data is stored in-memory via `ConcurrentHashMap` — no database required. The application runs on Render as a Java web service.

---

## 4. Key Features

### 4.1 Four Rotating Mission Types

Missions cycle in a fixed order: `SEAL_INTEL → CRACK_BROADCAST → TAMPER_HUNT → SECRET_DROP → repeat`.

| Mission              | Algorithm            | Points | Description                                                    |
|----------------------|----------------------|--------|----------------------------------------------------------------|
| **SEAL THE INTEL**   | Playfair encryption  | +10    | Encrypt a plaintext using a keyword and the 5x5 Playfair grid  |
| **CRACK THE CODE**   | Playfair decryption  | +15    | Decrypt an intercepted ciphertext given the keyword             |
| **FIND THE FAKE**    | SHA-256              | +20    | Identify the tampered package by comparing SHA-256 hashes       |
| **SECRET DROP**      | RSA-2048 + Playfair  | +25    | Unlock an RSA-encrypted keyword with browser badge, then decrypt|

### 4.2 Scoring System

- **Solve by hand:** Full mission points.
- **AUTO-SOLVE:** Half points (computer does the work).
- **Wrong answer:** Zero points, correct answer revealed. Nothing lost.
- Scores persist in memory for the lifetime of the server process.
- **TOP AGENTS** leaderboard shows top 20 by lifetime score.

### 4.3 Cryptographic Engines (Pure Java — No Third-Party Crypto Libraries)

| Engine                | Lines | Capabilities                                                      |
|-----------------------|-------|-------------------------------------------------------------------|
| **PlayfairEngine**    | 213   | 5x5 key matrix, bigram splitting, same-row/column/rectangle transforms |
| **AsymmetricEngine**  | 135   | RSA-2048 key generation, OAEP+SHA-256 encrypt/decrypt, Base64 encoding |
| **Sha256Engine**      | 37    | SHA-256 digest returning 64-char lowercase hex                    |
| **DeadDropProtocol**  | 52    | Canonical seal format, SHA-256 computation, constant-time verification |

All cryptographic implementations use only the Java standard library (`java.security`, `javax.crypto`) with no external dependencies.

### 4.4 Browser-Side RSA Badge System

- On first play, the browser generates an RSA-2048 key pair via the Web Crypto API.
- The public key is registered with the server; the private key stays in `localStorage`.
- For SECRET_DROP missions, the server encrypts the Playfair keyword under the agent's public badge.
- The browser unlocks it using the private key that never left the machine.
- Java's `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` and WebCrypto's `RSA-OAEP` + SHA-256 are wire-compatible.

### 4.5 Crypto Tool Endpoints (Free-Play)

| Endpoint                          | Description                     |
|-----------------------------------|---------------------------------|
| `GET /api/tools/playfair/grid`    | Live 5x5 key matrix for a keyword |
| `POST /api/tools/playfair/seal`   | Encrypt with Playfair            |
| `POST /api/tools/playfair/open`   | Decrypt with Playfair            |
| `POST /api/tools/sha256`          | Hash anything with SHA-256       |
| `POST /api/tools/rsa/wrap`        | Seal a secret under any public key |
| `POST /api/tools/rsa/unlock`      | Unwrap with a pasted private key |

---

## 5. REST API Summary

| Method | Endpoint                        | Description                    |
|--------|---------------------------------|--------------------------------|
| POST   | `/api/agents`                   | Enlist as a new agent          |
| GET    | `/api/agents/{codename}`        | Get agent profile / dossier    |
| PUT    | `/api/agents/{codename}/badge`  | Register RSA public key        |
| GET    | `/api/agents/leaderboard`       | Top 20 agents by score         |
| POST   | `/api/missions/draw`            | Draw a new mission             |
| POST   | `/api/missions/solve`           | Submit an answer               |
| GET    | `/api/tools/playfair/grid`      | Get Playfair key matrix        |
| POST   | `/api/tools/playfair/seal`      | Playfair encrypt               |
| POST   | `/api/tools/playfair/open`      | Playfair decrypt               |
| POST   | `/api/tools/sha256`             | SHA-256 hash                   |
| POST   | `/api/tools/rsa/wrap`           | RSA encrypt                    |
| POST   | `/api/tools/rsa/unlock`         | RSA decrypt                    |

---

## 6. Testing

### 6.1 Test Suite Overview

**4 test files, 24 test methods**, using JUnit 5 + AssertJ + Spring Boot MockMvc.

| Test File                          | Tests | Type        | Coverage                                               |
|------------------------------------|-------|-------------|--------------------------------------------------------|
| `PlayfairEngineTest.java`          | 8     | Unit        | Matrix formation, encrypt/decrypt vectors, padding, J→I normalization |
| `AsymmetricEngineTest.java`        | 6     | Unit        | RSA round-trip, OAEP WebCrypto compatibility, key encoding, oversized plaintext |
| `DeadDropProtocolTest.java`        | 4     | Unit        | SHA-256 vectors, seal determinism, tamper detection    |
| `GameFlowIntegrationTest.java`     | 6     | Integration | Full game loop: enlist, tools, SEAL INTEL, TAMPER HUNT, SECRET DROP, leaderboard |

### 6.2 Running Tests

```bash
./mvnw test
```

---

## 7. Configuration

### 7.1 Default Profile

- Server: `0.0.0.0:8080` (configurable via `$PORT`)
- Storage: In-memory (`ConcurrentHashMap`) — no database
- Hibernate/JPA: Not used
- Jackson: dates as ISO strings (not timestamps)

### 7.2 Deployment (Render)

Configured via `render.yaml`:

- Runtime: Java 17
- Build: `./mvnw clean package -DskipTests`
- Start: `java -jar target/blackout-1.0.0.jar`
- No database attachment required

---

## 8. How to Run

### Prerequisites
- Java 17 or later
- Maven 3.8+ (or use the included wrapper)

### Steps

```bash
# Clone the repository
git clone <repo-url>
cd blackout

# Run the application
./mvnw spring-boot:run

# Open in browser
open http://127.0.0.1:8080
```

---

## 9. Observations and Design Decisions

1. **No database** — All agent profiles and scores are stored in-memory via `ConcurrentHashMap` (`AgentStore`). Data lives for the lifetime of the server process. This eliminates database setup, connection pooling, and migration complexity.

2. **No external crypto libraries** — All three cryptographic engines are implemented from scratch using only the Java standard library, making the educational purpose transparent.

3. **In-memory mission state with TTL** — Active missions are held in a `ConcurrentHashMap` with a 15-minute expiry and are single-use, appropriate for a loopback game server.

4. **Browser-side key management** — RSA private keys never touch the server. The browser mints keys via WebCrypto and stores them in `localStorage`. This is a genuine asymmetric cryptography demonstration.

5. **Codename-only authentication** — Suitable for loopback/localhost play only. The README explicitly warns about this limitation.

---

## 10. Known Limitations

- **Playfair cipher is weak** — It is a classical cipher with well-known vulnerabilities (frequency analysis, digraph patterns). It is used here purely for educational value.
- **No real authentication** — Codename-only identification is not secure for multi-user or networked deployments.
- **No persistence** — All data is lost when the server restarts. Scores and agent profiles are not durable.
- **No cross-player leaderboard** — The leaderboard only shows agents from the current server session.
- **No CI/CD pipeline** — No automated build/test/deploy workflow is configured.

---

## 11. Conclusion

BLACKOUT is a well-structured, educational full-stack Java application that successfully combines game mechanics with real cryptographic concepts. The clean layered architecture, comprehensive test coverage (24 tests), and clever use of browser-side RSA badge minting make it both a functional game and a practical demonstration of cryptographic principles. With no database dependency, it deploys as a single JAR on Render with minimal configuration.
