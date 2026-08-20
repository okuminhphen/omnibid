package com.omnibid.auction.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibid.auction.domain.AuctionOutboxEvent;
import com.omnibid.auction.event.BidPlacedEvent;
import com.omnibid.auction.repository.AuctionOutboxEventRepository;
import com.omnibid.auction.service.KafkaProducerService;
import com.omnibid.auction.service.RabbitMQPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionOutboxPublisher {

    private final AuctionOutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;
    private final RabbitMQPublisherService rabbitMQPublisherService;

    @Scheduled(fixedDelayString = "${omnibid.outbox.fixed-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        for (AuctionOutboxEvent event : repository.lockNextUnpublishedBatch()) {
            try {
                publish(event);
                event.markPublished(Instant.now());
            } catch (RuntimeException exception) {
                event.markFailed(exception);
                log.warn(
                        "Auction outbox publish failed eventId={} type={} attempt={}",
                        event.getId(),
                        event.getEventType(),
                        event.getAttempts(),
                        exception
                );
                // Preserve event order and retry from this event on the next poll.
                return;
            }
        }
    }

    private void publish(AuctionOutboxEvent event) {
        try {
            switch (event.getEventType()) {
                case BID_PLACED -> kafkaProducerService.sendBidEvent(
                        objectMapper.readValue(event.getPayload(), BidPlacedEvent.class)
                );
                case REFUND_REQUESTED -> rabbitMQPublisherService.sendRefundCommand(
                        objectMapper.readValue(event.getPayload(), RefundCommand.class)
                );
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not deserialize auction outbox event " + event.getId(),
                    exception
            );
        }
    }
}
