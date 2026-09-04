package com.omnibid.auction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.omnibid.auction.domain.AuctionOutboxEvent;
import com.omnibid.auction.domain.AuctionOutboxEventType;
import com.omnibid.auction.event.BidPlacedEvent;
import com.omnibid.auction.messaging.RefundCommand;
import com.omnibid.auction.repository.AuctionOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuctionOutboxServiceTest {

    private AuctionOutboxEventRepository repository;
    private AuctionOutboxService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuctionOutboxEventRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new AuctionOutboxService(repository, objectMapper);
    }

    @Test
    void storesBidEventWithBidIdAsStableOutboxId() {
        UUID bidId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        BidPlacedEvent event = new BidPlacedEvent(
                bidId,
                auctionId,
                UUID.randomUUID(),
                new BigDecimal("120.00"),
                Instant.parse("2026-08-20T00:00:00Z")
        );

        service.enqueueBidPlaced(event);

        ArgumentCaptor<AuctionOutboxEvent> captor = ArgumentCaptor.forClass(AuctionOutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(bidId);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(auctionId);
        assertThat(captor.getValue().getEventType()).isEqualTo(AuctionOutboxEventType.BID_PLACED);
        assertThat(captor.getValue().getPayload()).contains(bidId.toString());
    }

    @Test
    void duplicateRefundUsesDeterministicOutboxId() {
        RefundCommand command = new RefundCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );
        when(repository.existsById(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        service.enqueueRefund(command);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
