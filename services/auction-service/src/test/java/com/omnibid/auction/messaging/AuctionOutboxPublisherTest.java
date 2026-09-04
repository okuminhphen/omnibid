package com.omnibid.auction.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.omnibid.auction.domain.AuctionOutboxEvent;
import com.omnibid.auction.domain.AuctionOutboxEventType;
import com.omnibid.auction.event.BidPlacedEvent;
import com.omnibid.auction.repository.AuctionOutboxEventRepository;
import com.omnibid.auction.service.KafkaProducerService;
import com.omnibid.auction.service.RabbitMQPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuctionOutboxPublisherTest {

    private AuctionOutboxEventRepository repository;
    private KafkaProducerService kafkaProducerService;
    private RabbitMQPublisherService rabbitMQPublisherService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository = mock(AuctionOutboxEventRepository.class);
        kafkaProducerService = mock(KafkaProducerService.class);
        rabbitMQPublisherService = mock(RabbitMQPublisherService.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void marksBidEventPublishedOnlyAfterKafkaAcknowledges() throws Exception {
        BidPlacedEvent payload = new BidPlacedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                Instant.parse("2026-08-20T00:00:00Z")
        );
        AuctionOutboxEvent event = AuctionOutboxEvent.pending(
                payload.bidId(),
                payload.auctionId(),
                AuctionOutboxEventType.BID_PLACED,
                objectMapper.writeValueAsString(payload),
                payload.timestamp()
        );
        when(repository.lockNextUnpublishedBatch()).thenReturn(List.of(event));

        new AuctionOutboxPublisher(
                repository,
                objectMapper,
                kafkaProducerService,
                rabbitMQPublisherService
        ).publishPending();

        verify(kafkaProducerService).sendBidEvent(payload);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttempts()).isZero();
        verifyNoInteractions(rabbitMQPublisherService);
    }

    @Test
    void recordsFailureAndLeavesEventPendingForRetry() throws Exception {
        BidPlacedEvent payload = new BidPlacedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                Instant.parse("2026-08-20T00:00:00Z")
        );
        AuctionOutboxEvent event = AuctionOutboxEvent.pending(
                payload.bidId(),
                payload.auctionId(),
                AuctionOutboxEventType.BID_PLACED,
                objectMapper.writeValueAsString(payload),
                payload.timestamp()
        );
        when(repository.lockNextUnpublishedBatch()).thenReturn(List.of(event));
        doThrow(new IllegalStateException("Kafka unavailable"))
                .when(kafkaProducerService).sendBidEvent(any());

        new AuctionOutboxPublisher(
                repository,
                objectMapper,
                kafkaProducerService,
                rabbitMQPublisherService
        ).publishPending();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("Kafka unavailable");
    }
}
