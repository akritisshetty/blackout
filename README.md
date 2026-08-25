# Blackout Basic

A minimal Spring Boot application hosting the Blackout cipher game.

## Run

```bash
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080/
```

## What it does

- **Playfair cipher** — 5x5 grid (I/J shared) encrypt/decrypt for SEAL and CRACK missions
- **RSA-2048 OAEP-SHA256** — browser badge (key pair in `localStorage`) unlocks SECRET DROP
- **SHA-256** — tamper-evident seal check for FIND THE FAKE
- 4 missions cycle forever with in-memory store and `localStorage` for agent/badge persistence
- Simple web UI at `/` and JSON API under `/api`
