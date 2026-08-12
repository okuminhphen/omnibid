package com.omnibid.audit.domain;

import com.omnibid.audit.messaging.BidPlacedEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "bid_logs")
@Getter
@Setter
@NoArgsConstructor
public class BidLog {

    @Id
    private UUID eventId;
    private int schemaVersion;
    private UUID bidId;
    private UUID auctionId;
    private UUID bidderId;
    private BigDecimal amount;
    private UUID walletTransactionId;
    private String correlationId;
    private Instant occurredAt;
    private Instant consumedAt;

    public static BidLog from(BidPlacedEvent event) {
        BidLog log = new BidLog();
        log.setEventId(event.eventId());
        log.setSchemaVersion(event.schemaVersion());
        log.setBidId(event.bidId());
        log.setAuctionId(event.auctionId());
        log.setBidderId(event.bidderId());
        log.setAmount(event.amount());
        log.setWalletTransactionId(event.walletTransactionId());
        log.setCorrelationId(event.correlationId());
        log.setOccurredAt(event.occurredAt());
        log.setConsumedAt(Instant.now());
        return log;
    }
}
