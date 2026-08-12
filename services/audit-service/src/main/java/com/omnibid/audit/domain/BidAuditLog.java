package com.omnibid.audit.domain;

import com.omnibid.audit.messaging.BidPlacedEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "bid_logs")
@CompoundIndex(
        name = "idx_bid_logs_auction_timestamp",
        def = "{'auctionId': 1, 'timestamp': -1}"
)
@Getter
@Setter
@NoArgsConstructor
public class BidAuditLog {

    /** bidId is the Mongo _id, making Kafka redelivery idempotent. */
    @Id
    private String id;

    private UUID auctionId;
    private UUID userId;
    private BigDecimal bidAmount;
    private Instant timestamp;

    public static BidAuditLog from(BidPlacedEvent event) {
        BidAuditLog log = new BidAuditLog();
        log.setId(event.bidId().toString());
        log.setAuctionId(event.auctionId());
        log.setUserId(event.userId());
        log.setBidAmount(event.bidAmount());
        log.setTimestamp(event.timestamp());
        return log;
    }
}
