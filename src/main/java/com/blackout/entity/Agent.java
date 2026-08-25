package com.blackout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * BLACKOUT // Agent
 *
 * A playable operative profile. Codename doubles as the login (this network lives on
 * loopback - there is nothing to authenticate against).
 *
 * {@code publicKey} holds the agent's RSA-2048 badge (Base64 X.509) minted automatically
 * in the browser via WebCrypto; the matching private key never leaves the browser.
 *
 * Stored in-memory via AgentStore (no database required).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {

    private Long id;

    /** Unique field identity, stored uppercase. */
    private String codename;

    /** Total points earned. */
    private int score;

    /** Missions solved. */
    private int missionsSolved;

    /** Missions answered wrong. */
    private int missionsFailed;

    /** RSA-2048 public badge, Base64 X.509. Null until first forge (automatic). */
    private String publicKey;

    private LocalDateTime createdAt;

    private LocalDateTime lastActiveAt;
}
