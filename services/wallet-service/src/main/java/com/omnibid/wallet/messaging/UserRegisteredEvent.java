package com.omnibid.wallet.messaging;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        String eventType,
        int schemaVersion,
        UUID userId,
        Instant occurredAt
) {
}
