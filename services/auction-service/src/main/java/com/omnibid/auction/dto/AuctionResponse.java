package com.omnibid.auction.dto;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.AuctionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AuctionResponse(
        UUID id,
        String title,
        AuctionStatus status,
        BigDecimal startingPrice,
        BigDecimal currentPrice,
        BigDecimal stepPrice,
        BigDecimal depositAmount,
        UUID winningUserId,
        Instant startTime,
        Instant endTime,
        // Backward-compatible aliases used by the existing Next.js UI.
        UUID highestBidderId,
        Instant endsAt,
        long version
) {
    public static AuctionResponse from(Auction auction) {
        return new AuctionResponse(
                auction.getId(),
                auction.getTitle(),
                auction.getStatus(),
                auction.getStartingPrice(),
                auction.getCurrentPrice(),
                auction.getStepPrice(),
                auction.getDepositAmount(),
                auction.getWinningUserId(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getWinningUserId(),
                auction.getEndTime(),
                auction.getVersion()
        );
    }
}
