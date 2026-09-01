package com.omnibid.auction.service;

import com.omnibid.auction.dto.BidResponse;
import com.omnibid.auction.dto.EndAuctionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/** Thin application facade kept as the stable boundary used by HTTP and schedulers. */
@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {
    private final PlaceBidUseCase placeBidUseCase;
    private final EndAuctionUseCase endAuctionUseCase;

    @Override
    public BidResponse placeBid(
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount,
            String idempotencyKey
    ) {
        return placeBidUseCase.execute(auctionId, userId, bidAmount, idempotencyKey);
    }

    @Override
    public EndAuctionResponse endAuction(UUID auctionId) {
        return endAuctionUseCase.execute(auctionId);
    }
}
