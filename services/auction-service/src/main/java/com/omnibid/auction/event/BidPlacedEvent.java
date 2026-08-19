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
        Instant timestamp
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
