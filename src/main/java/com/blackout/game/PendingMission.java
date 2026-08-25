package com.blackout.game;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * BLACKOUT // PendingMission
 *
 * Server-side state for one active mission. The challenge data shown to the agent is
 * safe to send; the expected answers stay here on the server.
 */
@Getter
@Builder
public class PendingMission {

    /** One intercepted package used by TAMPER_HUNT missions. */
    public record AuditPackage(long id, String payload, String keyBlob, String seal, boolean tampered) {
    }

    private final String token;
    private final MissionType type;
    private final String agentCodename;
    private final Instant createdAt;
    private final Instant expiresAt;

    /** Client-facing challenge block - contains no answers. */
    private final Map<String, Object> challenge;

    /** The answer the agent must reproduce. */
    private final String expectedAnswer;

    /** Alternate accepted form (readable phrase without padding) for decrypt missions. */
    private final String expectedAlternative;

    /** Packages with their secret tamper flags (TAMPER_HUNT only). */
    private final List<AuditPackage> auditPackages;

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
