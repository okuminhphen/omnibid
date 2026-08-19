package com.omnibid.identity.messaging;

import com.omnibid.identity.config.IdentityProperties;
import com.omnibid.identity.domain.IdentityOutboxEvent;
import com.omnibid.identity.repository.IdentityOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityOutboxPublisher {

    private final IdentityOutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final IdentityProperties properties;

    @Scheduled(fixedDelayString = "${omnibid.identity.outbox-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        for (IdentityOutboxEvent event : repository
                .findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafkaTemplate.send(
                                properties.kafka().identityTopic(),
                                event.getAggregateId().toString(),
                                event.getPayload()
                        )
                        .get(5, TimeUnit.SECONDS);
                event.setPublishedAt(Instant.now());
            } catch (Exception exception) {
                log.warn("Identity outbox publish failed for event {}", event.getId(), exception);
                throw new IllegalStateException("Identity outbox publish failed", exception);
            }
        }
    }
}
