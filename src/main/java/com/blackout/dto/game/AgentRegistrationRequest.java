package com.blackout.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound request: enlist (or resume) an agent by codename. Codename doubles as the
 * login on this loopback network.
 */
public record AgentRegistrationRequest(

        @NotBlank(message = "codename is required")
        @Size(min = 3, max = 40, message = "codename must be 3-40 characters")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "codename allows letters, digits, dash and underscore only")
        String codename) {
}
