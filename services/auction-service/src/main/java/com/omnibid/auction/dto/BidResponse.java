package com.omnibid.auction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidResponse(
        UUID bidId,
        UUID auctionId,
        UUID bidderId,
        BigDecimal amount,
        UUID walletTransactionId,
        Instant placedAt
) {
}
