package com.blackout.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Field-terminal request: seal a message under a keyword with the Playfair engine. */
public record ToolPlayfairSealRequest(
        @NotBlank(message = "message is required") @Size(max = 2048) String message,
        @NotBlank(message = "keyword is required") @Size(max = 120) String keyword) {
}
