package com.omnibid.auction.service;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.AuctionStatus;
import com.omnibid.auction.repository.AuctionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuctionEndSchedulerTest {

    @Test
    void delegatesExpiredActiveAuctionToLockedEndWorkflow() {
        AuctionRepository repository = mock(AuctionRepository.class);
        AuctionService service = mock(AuctionService.class);
        Auction auction = new Auction();
        auction.setId(UUID.randomUUID());
        when(repository.findTop100ByStatusAndEndTimeLessThanEqualOrderByEndTimeAsc(
                eq(AuctionStatus.ACTIVE),
                any(Instant.class)
        )).thenReturn(List.of(auction));

        new AuctionEndScheduler(repository, service).endExpiredAuctions();

        verify(service).endAuction(auction.getId());
    }
}
