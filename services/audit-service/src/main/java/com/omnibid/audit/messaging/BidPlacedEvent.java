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
        @JsonAlias("occurredAt") Instant timestamp
) {
}
