package com.omnibid.auction.service;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.AuctionStatus;
import com.omnibid.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEndScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    @Scheduled(fixedDelayString = "${omnibid.auction.auto-end-delay-ms:5000}")
    public void endExpiredAuctions() {
        for (Auction auction : auctionRepository
                .findTop100ByStatusAndEndTimeLessThanEqualOrderByEndTimeAsc(
                        AuctionStatus.ACTIVE,
                        Instant.now()
                )) {
            try {
                // endAuction owns the per-auction Redis lock. Multiple scheduler
                // instances can safely discover the same expired auction.
                auctionService.endAuction(auction.getId());
            } catch (RuntimeException exception) {
                log.warn("Could not auto-end auction {}", auction.getId(), exception);
            }
        }
    }
}
