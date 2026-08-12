package com.omnibid.audit.messaging;

import com.omnibid.audit.domain.BidLog;
import com.omnibid.audit.repository.BidLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BidAuditConsumer {

    private final BidLogRepository repository;

    @KafkaListener(topics = "${omnibid.kafka.bid-topic}")
    public void consume(BidPlacedEvent event) {
        if (repository.existsById(event.eventId())) {
            log.debug("Ignoring duplicate bid event {}", event.eventId());
            return;
        }

        try {
            repository.insert(BidLog.from(event));
        } catch (DuplicateKeyException ignored) {
            log.debug("Bid event {} was inserted by another consumer", event.eventId());
        }
    }
}
