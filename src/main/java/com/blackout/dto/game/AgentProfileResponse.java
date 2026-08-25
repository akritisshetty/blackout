package com.blackout.dto.game;

import com.blackout.entity.Agent;

import java.time.LocalDateTime;

/**
 * Outbound agent profile: the numbers shown in the header chip.
 */
public record AgentProfileResponse(
        String codename,
        int score,
        int missionsSolved,
        int missionsFailed,
        boolean hasBadge,
        LocalDateTime createdAt) {

    public static AgentProfileResponse from(Agent agent) {
        return new AgentProfileResponse(
                agent.getCodename(),
                agent.getScore(),
                agent.getMissionsSolved(),
                agent.getMissionsFailed(),
                agent.getPublicKey() != null,
                agent.getCreatedAt());
    }
}
