package com.omnibid.audit.domain;

import com.omnibid.audit.messaging.BidPlacedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BidAuditLogTest {

    @Test
    void usesBidIdAsMongoIdForIdempotentKafkaRedelivery() {
        UUID bidId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        BidPlacedEvent event = new BidPlacedEvent(
                bidId,
                auctionId,
                userId,
                new BigDecimal("300.00"),
                timestamp
        );

        BidAuditLog log = BidAuditLog.from(event);

        assertThat(log.getId()).isEqualTo(bidId.toString());
        assertThat(log.getAuctionId()).isEqualTo(auctionId);
        assertThat(log.getUserId()).isEqualTo(userId);
        assertThat(log.getBidAmount()).isEqualByComparingTo("300.00");
        assertThat(log.getTimestamp()).isEqualTo(timestamp);
    }
}
