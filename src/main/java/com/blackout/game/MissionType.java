package com.blackout.game;

/**
 * BLACKOUT // MissionType
 *
 * The four mission classes. One simple loop, one of each, forever:
 * encrypt -> decrypt -> find the fake -> secret drop.
 */
public enum MissionType {

    SEAL_INTEL(
            "SEAL THE INTEL",
            "Playfair encryption",
            "Encrypt the message with the keyword. Use the grid: same row = step right, "
                    + "same column = step down, otherwise swap columns.",
            10),

    CRACK_BROADCAST(
            "CRACK THE CODE",
            "Playfair decryption",
            "You know the keyword. Decrypt the message by running the grid rules backwards.",
            15),

    TAMPER_HUNT(
            "FIND THE FAKE",
            "SHA-256 integrity",
            "Exactly ONE package below was secretly altered after it was sealed. "
                    + "Press HASH on each and pick the fake.",
            20),

    SECRET_DROP(
            "SECRET DROP",
            "RSA-2048 + Playfair",
            "The keyword is locked with YOUR badge. Press UNLOCK, then decrypt the message.",
            25);

    private final String title;
    private final String cryptoLayer;
    private final String briefing;
    private final int points;

    MissionType(String title, String cryptoLayer, String briefing, int points) {
        this.title = title;
        this.cryptoLayer = cryptoLayer;
        this.briefing = briefing;
        this.points = points;
    }

    public String title() {
        return title;
    }

    /** Short name of the algorithm(s) this mission exercises. */
    public String cryptoLayer() {
        return cryptoLayer;
    }

    public String briefing() {
        return briefing;
    }

    /** Points for solving by hand (AUTO-solve pays half). */
    public int points() {
        return points;
    }
}
