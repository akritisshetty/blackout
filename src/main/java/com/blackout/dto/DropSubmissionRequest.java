package com.blackout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound transmission envelope posted by the field terminal when burying a dead drop.
 * The payload arrives already Playfair-encrypted and the key already RSA-wrapped -
 * the backend never sees plaintext intel.
 */
public record DropSubmissionRequest(

        @NotBlank(message = "codename is required")
        @Size(max = 120, message = "codename must not exceed 120 characters")
        String codename,

        @Size(max = 120, message = "locationTag must not exceed 120 characters")
        String locationTag,

        @NotBlank(message = "encryptedPayload is required")
        String encryptedPayload,

        @NotBlank(message = "encryptedKey is required")
        @Size(max = 4096, message = "encryptedKey blob too large")
        String encryptedKey) {
}
