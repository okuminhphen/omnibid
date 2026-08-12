package com.omnibid.auction.dto;

import com.omnibid.auction.domain.AuctionStatus;

import java.util.UUID;

public record EndAuctionResponse(
        UUID auctionId,
        AuctionStatus status,
        UUID winningUserId,
        int refundCommandsPublished
) {
}
