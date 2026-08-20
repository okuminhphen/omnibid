package com.omnibid.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auction_outbox_events")
@Getter
@NoArgsConstructor
public class AuctionOutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuctionOutboxEventType eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    public static AuctionOutboxEvent pending(
            UUID id,
            UUID aggregateId,
            AuctionOutboxEventType eventType,
            String payload,
            Instant createdAt
    ) {
        AuctionOutboxEvent event = new AuctionOutboxEvent();
        event.id = id;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = payload;
        event.createdAt = createdAt;
        return event;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(Throwable error) {
        attempts++;
        String message = error.getMessage() == null
                ? error.getClass().getSimpleName()
                : error.getMessage();
        lastError = message.substring(0, Math.min(message.length(), 1000));
    }
}
