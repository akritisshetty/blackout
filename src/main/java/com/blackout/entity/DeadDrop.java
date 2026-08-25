package com.blackout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single buried "dead drop": an inter-agent package holding the Playfair-sealed
 * message, the RSA-wrapped keyword, and the SHA-256 digital seal computed at burial
 * time. The {@code sha256Seal} column is written exactly once and treated as immutable
 * afterwards (enforced by convention and asserted in tests).
 */
@Entity
@Table(name = "dead_drops")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadDrop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Operation label, e.g. "OPERATION-NIGHTFALL". */
    @Column(nullable = false, length = 120)
    private String codename;

    /** Physical drop-point tag, e.g. "LISBON DOCK 7". */
    @Column(length = 120)
    private String locationTag;

    /** Playfair ciphertext - potentially long, hence LOB storage. */
    @Lob
    private String encryptedPayload;

    /** RSA-OAEP wrapped Playfair keyword, Base64 encoded. */
    @Column(nullable = false, length = 4096)
    private String encryptedKey;

    /** SHA-256 hex digest over payload|key, stamped once by the backend. */
    @Column(nullable = false, length = 64)
    private String sha256Seal;

    /** Moment of burial. */
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
