package com.omnibid.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank String credential,
        @NotBlank String nonce
) {
}
