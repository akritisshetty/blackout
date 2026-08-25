package com.blackout.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Inbound mission solution. Only the fields relevant to the mission type are read:
 *
 *   SEAL_INTEL      -> cipherText
 *   CRACK_BROADCAST -> plainText
 *   TAMPER_HUNT     -> flaggedTamperedIds
 *   SECRET_DROP     -> plainText
 *
 * {@code assisted} = the AUTO button was used (half points).
 */
public record SolveRequest(

        @NotBlank(message = "token is required")
        String token,

        @Size(max = 4096, message = "plainText answer too large")
        String plainText,

        @Size(max = 8192, message = "cipherText answer too large")
        String cipherText,

        List<Long> flaggedTamperedIds,

        boolean assisted) {
}
