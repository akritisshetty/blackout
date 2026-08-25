package com.blackout.game;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BLACKOUT // MissionSessionStore
 *
 * Holds live mission attempts in memory, keyed by an unguessable token. Missions expire
 * after {@link #MISSION_TTL} - burned or abandoned operations are swept on access.
 * Loopback game server: no persistence needed for pending attempts (solved missions
 * are what the H2 dossier remembers).
 */
@Component
public class MissionSessionStore {

    public static final Duration MISSION_TTL = Duration.ofMinutes(15);

    private final Map<String, PendingMission> active = new ConcurrentHashMap<>();

    /** Registers a freshly generated mission and returns its bearer token. */
    public String put(PendingMission mission) {
        sweep();
        active.put(mission.getToken(), mission);
        return mission.getToken();
    }

    /**
     * Fetches a live mission. Expired tokens answer empty - the caller reports the
     * mission as burned.
     */
    public PendingMission get(String token) {
        if (token == null) {
            return null;
        }
        PendingMission mission = active.get(token);
        if (mission == null) {
            return null;
        }
        if (mission.isExpired(Instant.now())) {
            active.remove(token);
            return null;
        }
        return mission;
    }

    public void remove(String token) {
        active.remove(token);
    }

    public int size() {
        return active.size();
    }

    private void sweep() {
        Instant now = Instant.now();
        active.values().removeIf(mission -> mission.isExpired(now));
    }
}
