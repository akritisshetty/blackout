package com.blackout.dto;

import com.blackout.entity.DeadDrop;

import java.time.LocalDateTime;

/**
 * Outbound projection served to wiretap clients. Mirrors the persisted entity 1:1 so
 * the UI can independently re-compute the SHA-256 seal over payload|key and compare
 * against {@code sha256Seal} - the heart of the tamper-evident workflow.
 */
public record DropResponse(
        Long id,
        String codename,
        String locationTag,
        String encryptedPayload,
        String encryptedKey,
        String sha256Seal,
        LocalDateTime timestamp) {

    public static DropResponse from(DeadDrop drop) {
        return new DropResponse(
                drop.getId(),
                drop.getCodename(),
                drop.getLocationTag(),
                drop.getEncryptedPayload(),
                drop.getEncryptedKey(),
                drop.getSha256Seal(),
                drop.getTimestamp());
    }
}
