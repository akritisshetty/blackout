package com.blackout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
 */
@Entity
@Table(name = "agents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique field identity, stored uppercase. */
    @Column(nullable = false, unique = true, length = 40)
    private String codename;

    /** Total points earned. */
    @Column(nullable = false)
    private int score;

    /** Missions solved. */
    @Column(nullable = false)
    private int missionsSolved;

    /** Missions answered wrong. */
    @Column(nullable = false)
    private int missionsFailed;

    /** RSA-2048 public badge, Base64 X.509. Null until first forge (automatic). */
    @Column(length = 512)
    private String publicKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastActiveAt;
}
