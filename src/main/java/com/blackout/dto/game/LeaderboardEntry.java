package com.blackout.dto.game;

import com.blackout.entity.Agent;

/**
 * One row of the leaderboard.
 */
public record LeaderboardEntry(
        int position,
        String codename,
        int score,
        int missionsSolved) {

    public static LeaderboardEntry from(Agent agent, int position) {
        return new LeaderboardEntry(
                position,
                agent.getCodename(),
                agent.getScore(),
                agent.getMissionsSolved());
    }
}
