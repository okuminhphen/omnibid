package com.omnibid.audit.messaging;

import com.omnibid.audit.domain.BidAuditLog;
import com.omnibid.audit.repository.BidAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BidKafkaConsumer {

    private final BidAuditLogRepository repository;

    @KafkaListener(
            topics = "${omnibid.kafka.bid-topic:bid-events}",
            groupId = "${omnibid.kafka.consumer-group:audit-group}",
            containerFactory = "bidKafkaListenerContainerFactory"
    )
    public void consume(BidPlacedEvent event) {
        if (event == null || !event.hasSupportedSchema()) {
            throw new IllegalArgumentException("Unsupported BidPlaced event schema");
        }
        try {
            repository.insert(BidAuditLog.from(event));
            log.debug("Stored audit log for bid {}", event.bidId());
        } catch (DuplicateKeyException ignored) {
            // Mongo _id = bidId. A redelivered Kafka record is safe to acknowledge.
            log.debug("Ignoring duplicate bid event {}", event.bidId());
        }
    }
}
