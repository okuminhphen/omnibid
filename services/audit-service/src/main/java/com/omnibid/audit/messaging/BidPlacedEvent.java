package com.omnibid.audit.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidPlacedEvent(
        UUID eventId,
        int schemaVersion,
        UUID bidId,
        UUID auctionId,
        UUID bidderId,
        BigDecimal amount,
        UUID walletTransactionId,
        String correlationId,
        Instant occurredAt
) {
}
