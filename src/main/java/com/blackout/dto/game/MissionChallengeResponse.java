package com.blackout.dto.game;

import com.blackout.game.MissionType;
import com.blackout.game.PendingMission;

/**
 * Outbound mission challenge. {@code data} is type-specific (message, keyword, grid
 * inputs, packages, RSA blob) - everything the agent needs, none of the answers.
 */
public record MissionChallengeResponse(
        String token,
        MissionType type,
        String typeTitle,
        String cryptoLayer,
        String briefing,
        int points,
        long expiresInSeconds,
        java.util.Map<String, Object> data) {

    public static MissionChallengeResponse from(PendingMission mission) {
        return new MissionChallengeResponse(
                mission.getToken(),
                mission.getType(),
                mission.getType().title(),
                mission.getType().cryptoLayer(),
                mission.getType().briefing(),
                mission.getType().points(),
                java.time.Duration.between(java.time.Instant.now(), mission.getExpiresAt()).toSeconds(),
                mission.getChallenge());
    }
}
