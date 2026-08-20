package com.omnibid.auction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibid.auction.domain.AuctionOutboxEvent;
import com.omnibid.auction.domain.AuctionOutboxEventType;
import com.omnibid.auction.event.BidPlacedEvent;
import com.omnibid.auction.messaging.RefundCommand;
import com.omnibid.auction.repository.AuctionOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionOutboxService {

    private final AuctionOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public void enqueueBidPlaced(BidPlacedEvent event) {
        repository.save(AuctionOutboxEvent.pending(
                event.bidId(),
                event.auctionId(),
                AuctionOutboxEventType.BID_PLACED,
                serialize(event),
                event.timestamp()
        ));
    }

    public void enqueueRefund(RefundCommand command) {
        UUID outboxId = UUID.nameUUIDFromBytes(
                ("refund:" + command.transactionId()).getBytes(StandardCharsets.UTF_8)
        );
        if (repository.existsById(outboxId)) {
            return;
        }
        repository.save(AuctionOutboxEvent.pending(
                outboxId,
                command.auctionId(),
                AuctionOutboxEventType.REFUND_REQUESTED,
                serialize(command),
                Instant.now()
        ));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize auction outbox payload", exception);
        }
    }
}
