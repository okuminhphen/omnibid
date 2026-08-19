package com.omnibid.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 30) String phoneNumber,
        @NotBlank @Size(max = 20) String locale,
        @NotBlank @Size(max = 50) String timezone,
        @Size(max = 500) String bio
) {
}
