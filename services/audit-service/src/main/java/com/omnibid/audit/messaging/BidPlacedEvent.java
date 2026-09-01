package com.omnibid.audit.messaging;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidPlacedEvent(
        UUID bidId,
        UUID auctionId,
        @JsonAlias("bidderId") UUID userId,
        @JsonAlias("amount") BigDecimal bidAmount,
        @JsonAlias("occurredAt") Instant timestamp,
        int schemaVersion
) {
    public BidPlacedEvent(
            UUID bidId,
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount,
            Instant timestamp
    ) {
        this(bidId, auctionId, userId, bidAmount, timestamp, 1);
    }

    public boolean hasSupportedSchema() {
        // Version 0 is accepted for events emitted before explicit versioning was added.
        return schemaVersion == 0 || schemaVersion == 1;
    }
}
