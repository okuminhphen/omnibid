package com.omnibid.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record DevLoginRequest(@NotBlank String alias) {
}
