package com.omnibid.auction.dto;

import com.omnibid.auction.domain.Bid;

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
    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuctionId(),
                bid.getBidderId(),
                bid.getAmount(),
                bid.getWalletTransactionId(),
                bid.getPlacedAt()
        );
    }
}
