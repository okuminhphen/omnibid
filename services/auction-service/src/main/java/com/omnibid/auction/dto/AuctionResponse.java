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
        BigDecimal currentPrice,
        UUID highestBidderId,
        Instant endsAt,
        long version
) {
    public static AuctionResponse from(Auction auction) {
        return new AuctionResponse(
                auction.getId(),
                auction.getTitle(),
                auction.getStatus(),
                auction.getCurrentPrice(),
                auction.getHighestBidderId(),
                auction.getEndsAt(),
                auction.getVersion()
        );
    }
}
