package com.blackout.game;

import com.blackout.crypto.PlayfairEngine;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BLACKOUT // MissionBank
 *
 * Procedural mission material: short intel phrases and cipher keywords, plus dressing
 * (operation codenames, drop locations). Everything is letter-safe for the Playfair
 * engine - no digits, punctuation or J.
 */
public final class MissionBank {

    /** Short, hand-encryptable snippets. */
    private static final List<String> PHRASES = List.of(
            "meet at dock seven",
            "the falcon flies at dawn",
            "burn the safe house",
            "cargo moves at midnight",
            "the raven knows the harbor",
            "keep the black box cold",
            "shadow the red courier",
            "the package is a decoy",
            "trust the man in grey",
            "radio silence ends at four"
    );

    private static final List<String> KEYWORDS = List.of(
            "monarch", "nightowl", "velvet", "quartz", "kestrel",
            "harbor", "lantern", "sparrow", "cobalt", "talon"
    );

    private static final List<String> OPERATIONS = List.of(
            "operation-nightfall", "operation-glasshouse", "operation-ironveil",
            "operation-papermoon", "operation-sablefox", "operation-coldbarrel"
    );

    private MissionBank() {
        // static factory - never instantiated
    }

    public static String randomPhrase() {
        return PHRASES.get(ThreadLocalRandom.current().nextInt(PHRASES.size()));
    }

    public static String randomKeyword() {
        return KEYWORDS.get(ThreadLocalRandom.current().nextInt(KEYWORDS.size()));
    }

    public static String randomOperation() {
        return OPERATIONS.get(ThreadLocalRandom.current().nextInt(OPERATIONS.size()));
    }

    /** Uppercase A-Z-only form of a phrase - what agents type back as their answer. */
    public static String sanitize(String raw) {
        return PlayfairEngine.normalize(raw);
    }
}
