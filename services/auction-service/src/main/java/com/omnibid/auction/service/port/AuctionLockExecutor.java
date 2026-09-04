package com.omnibid.auction.service.port;

import java.util.UUID;
import java.util.function.Supplier;

/** Serializes commands for one auction aggregate across all service instances. */
public interface AuctionLockExecutor {
    <T> T execute(UUID auctionId, Supplier<T> action);
}
