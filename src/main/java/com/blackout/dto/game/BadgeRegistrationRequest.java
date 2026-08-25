package com.blackout.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound request: register an RSA-2048 public badge (Base64 X.509) against an agent.
 * The private half never leaves the agent's browser - the relay can wrap secrets for
 * this agent but can never unwrap them.
 */
public record BadgeRegistrationRequest(

        @NotBlank(message = "publicKey is required")
        @Size(max = 512, message = "publicKey blob too large for a 2048-bit X.509 key")
        String publicKey) {
}
