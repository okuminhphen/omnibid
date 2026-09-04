package com.omnibid.auction.service.port;

import java.math.BigDecimal;
import java.util.UUID;

/** Disposable read optimization. PostgreSQL remains the source of truth. */
public interface AuctionPriceCache {
    void put(UUID auctionId, BigDecimal currentPrice);
}
