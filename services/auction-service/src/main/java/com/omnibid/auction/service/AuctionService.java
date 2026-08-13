package com.omnibid.auction.service;

import com.omnibid.auction.dto.BidResponse;
import com.omnibid.auction.dto.EndAuctionResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface AuctionService {

    BidResponse placeBid(
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount,
            String idempotencyKey
    );

    EndAuctionResponse endAuction(UUID auctionId);
}
