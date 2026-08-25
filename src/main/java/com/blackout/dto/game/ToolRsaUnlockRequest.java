package com.blackout.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Field-terminal request: unwrap an RSA blob with a pasted private key. This is the
 * assisted path - agents who hold their badge privately never need it.
 */
public record ToolRsaUnlockRequest(
        @NotBlank(message = "blob is required") @Size(max = 4096) String blob,
        @NotBlank(message = "privateKey is required") @Size(max = 4096) String privateKey) {
}
