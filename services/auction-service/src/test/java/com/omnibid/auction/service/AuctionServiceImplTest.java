package com.omnibid.auction.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuctionServiceImplTest {
    @Test
    void delegatesCommandsToFocusedUseCases() {
        PlaceBidUseCase placeBid = mock(PlaceBidUseCase.class);
        EndAuctionUseCase endAuction = mock(EndAuctionUseCase.class);
        AuctionService service = new AuctionServiceImpl(placeBid, endAuction);
        UUID auctionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        service.placeBid(auctionId, userId, amount, "request-key");
        service.endAuction(auctionId);

        verify(placeBid).execute(auctionId, userId, amount, "request-key");
        verify(endAuction).execute(auctionId);
    }
}
