package com.blackout.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Field-terminal request: wrap a short secret under any RSA public badge. */
public record ToolRsaWrapRequest(
        @NotBlank(message = "publicKey is required") @Size(max = 512) String publicKey,
        @NotBlank(message = "secret is required") @Size(max = 190, message = "secret exceeds OAEP capacity (190 bytes)") String secret) {
}
