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
        String leadingBidderAlias,
        Instant startTime,
        Instant endTime,
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
                BidderAlias.from(auction.getId(), auction.getWinningUserId()),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getVersion()
        );
    }
}
