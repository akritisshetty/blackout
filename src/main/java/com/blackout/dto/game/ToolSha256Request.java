package com.blackout.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Field-terminal request: SHA-256 digest of an arbitrary string. */
public record ToolSha256Request(
        @NotBlank(message = "input is required")
        @Size(max = 8192, message = "input too large for the hash console") String input) {
}
