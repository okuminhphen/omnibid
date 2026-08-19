package com.omnibid.identity.dto;

import com.omnibid.identity.domain.AuthSession;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String userAgent,
        String status,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        boolean current
) {
    public static SessionResponse from(AuthSession session, UUID currentSessionId) {
        return new SessionResponse(
                session.getId(),
                session.getUserAgent(),
                session.getStatus().name(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getExpiresAt(),
                session.getId().equals(currentSessionId)
        );
    }
}
