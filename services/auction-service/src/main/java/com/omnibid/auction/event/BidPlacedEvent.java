package com.omnibid.auction.event;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidPlacedEvent(
        UUID bidId,
        UUID auctionId,
        UUID userId,
        BigDecimal bidAmount,
        Instant timestamp,
        int schemaVersion
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public BidPlacedEvent(
            UUID bidId,
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount,
            Instant timestamp
    ) {
        this(bidId, auctionId, userId, bidAmount, timestamp, 1);
    }
}
