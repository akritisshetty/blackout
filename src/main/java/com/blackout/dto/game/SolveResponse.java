package com.blackout.dto.game;

import com.blackout.entity.Agent;

/**
 * Outbound mission resolution - deliberately tiny:
 * did you get it, how many points, and what was the right answer if you didn't.
 */
public record SolveResponse(
        boolean correct,
        int pointsAwarded,
        int totalScore,
        int missionsSolved,
        String message,
        String expectedAnswer) {

    public static SolveResponse failed(String message, String expectedAnswer, Agent agent) {
        return new SolveResponse(false, 0, agent.getScore(),
                agent.getMissionsSolved(), message, expectedAnswer);
    }
}
