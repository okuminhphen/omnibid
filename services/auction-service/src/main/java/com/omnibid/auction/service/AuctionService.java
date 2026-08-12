package com.omnibid.auction.service;

import com.omnibid.auction.dto.BidResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface AuctionService {

    BidResponse placeBid(UUID auctionId, UUID userId, BigDecimal bidAmount);
}
