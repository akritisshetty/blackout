# ▚▞ BLACKOUT ▚▞

**The Cipher Game — a simple full stack Java web game that teaches cryptography by making you use it.**

One screen, one mission at a time. Four mission types repeat forever:

| # | Mission | Algorithm | Points |
|---|---|---|---|
| 1 | SEAL THE INTEL | **Playfair** encryption | +10 |
| 2 | CRACK THE CODE | **Playfair** decryption | +15 |
| 3 | FIND THE FAKE | **SHA-256** tamper detection | +20 |
| 4 | SECRET DROP | **RSA-2048** unlock + Playfair | +25 |

Solve by hand for full points or press AUTO-SOLVE for half. Wrong answers reveal the correct answer and cost nothing. Scores persist in H2 (in-memory) with a TOP AGENTS leaderboard.

> **How to play:** [INSTRUCTIONS.md](INSTRUCTIONS.md) — it fits on one page.

## Running

```bash
mvn spring-boot:run
# open http://127.0.0.1:8080
```

## Architecture

Single Spring Boot fat jar serving both the JSON API and the static game client.

```
src/main/java/com/blackout/
├── crypto/        ★ the three pure engines: PlayfairEngine, AsymmetricEngine (RSA-2048/OAEP), Sha256Engine (+ DeadDropProtocol)
├── game/          MissionType · MissionBank · PendingMission · MissionSessionStore
├── entity/        Agent · DeadDrop (JPA/H2)
├── repository/    derived-query repositories
├── dto/, dto/game/
├── service/       AgentService · MissionService · DeadDropService
└── controller/    agents · missions · tools · drops
src/main/resources/static/   index.html · css/blackout.css · js/{api,ui,badge,game}.js
```

The neat part: your RSA badge is minted **in the browser** (WebCrypto, automatic — no buttons) and registered by its public half only. The relay locks mission keywords under that public key; your browser unlocks them with the private key that never left your machine. Java's `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` and WebCrypto's `RSA-OAEP` + SHA-256 are wire-compatible.

## REST API

Base: `http://127.0.0.1:8080`

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/agents` | enlist/resume by codename |
| `GET` | `/api/agents/{codename}` | profile |
| `PUT` | `/api/agents/{codename}/badge` | register an RSA public badge |
| `GET` | `/api/leaderboard` | top agents |
| `POST` | `/api/missions/{codename}/new?type=` | draw a mission (`SEAL_INTEL`, `CRACK_BROADCAST`, `TAMPER_HUNT`, `SECRET_DROP`) |
| `POST` | `/api/missions/{codename}/solve` | submit a solution (`"assisted":true` = half points) |
| `GET/POST` | `/api/tools/playfair/grid`, `/seal`, `/open`, `/api/tools/sha256`, `/api/tools/rsa/wrap`, `/unlock` | crypto tools used by the UI |
| `POST/GET` | `/api/drops`, `/api/drops/{id}`, `/{id}/tamper` | dead-drop sandbox API (original project endpoints) |

## Tests

26 tests, all green: engine vectors & rules (17), dead-drop lifecycle (3), full game loop over MockMvc (6).

## Honest disclaimers

- Playfair is 19th-century pedagogy; never protect anything real with it.
- Codename-only login is fine on loopback and nowhere else.
- Raw SHA-256 seals are not keyed MACs; RSA wraps only a short keyword here.

## Enigma — Local Helper (clone-only)

The `enigma/` directory will help users to get the correct answer of any algorithm. It helps users who don't know how cryptography algorithms work or who want to know the answers to the questions. It is just for help.

> Only users who **clone this project and run it on their system** will get the benefits of Enigma. Users who use only the hosted website will not — the full repo (including `enigma/`) is not exposed there.

Run locally:
```bash
cd enigma && javac *.java && java MainUI
```

## Tech stack

Java 17+ · Spring Boot 3.3 (Web, JPA, Validation) · H2 · vanilla HTML/CSS/JS + WebCrypto · Lombok · JUnit 5 / MockMvc · Maven
