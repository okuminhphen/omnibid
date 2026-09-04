package com.omnibid.auction.dto;

import com.omnibid.auction.domain.Bid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Public projection deliberately excludes user and wallet transaction identifiers. */
public record BidHistoryResponse(
        UUID bidId,
        UUID auctionId,
        String bidderAlias,
        BigDecimal amount,
        Instant placedAt
) {
    public static BidHistoryResponse from(Bid bid) {
        return new BidHistoryResponse(
                bid.getId(),
                bid.getAuctionId(),
                BidderAlias.from(bid.getAuctionId(), bid.getBidderId()),
                bid.getAmount(),
                bid.getPlacedAt()
        );
    }
}
