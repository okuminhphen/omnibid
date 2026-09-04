package com.omnibid.auction.dto;

import com.omnibid.auction.domain.Bid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidResponse(
        UUID bidId,
        UUID auctionId,
        BigDecimal amount,
        Instant placedAt
) {
    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuctionId(),
                bid.getAmount(),
                bid.getPlacedAt()
        );
    }
}
