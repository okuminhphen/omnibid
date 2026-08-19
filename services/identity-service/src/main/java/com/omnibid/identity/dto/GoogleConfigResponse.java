package com.omnibid.identity.dto;

public record GoogleConfigResponse(
        boolean enabled,
        String clientId,
        String nonce
) {
}
