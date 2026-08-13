package com.omnibid.auction.controller;

import com.omnibid.auction.dto.PlaceBidRequest;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.repository.BidRepository;
import com.omnibid.auction.service.AuctionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionControllerTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private AuctionController controller;

    @Test
    void forwardsClientIdempotencyKeyToCoreBidService() {
        UUID auctionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("120.00");

        controller.placeBid(
                auctionId,
                "frontend-request-key",
                new PlaceBidRequest(userId, amount)
        );

        verify(auctionService).placeBid(
                auctionId,
                userId,
                amount,
                "frontend-request-key"
        );
    }
}
