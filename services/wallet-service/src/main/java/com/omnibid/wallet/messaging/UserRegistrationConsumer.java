package com.omnibid.wallet.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnibid.wallet.service.WalletAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationConsumer {

    private final ObjectMapper objectMapper;
    private final WalletAccountService walletAccountService;

    @KafkaListener(
            topics = "${omnibid.kafka.identity-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) {
        try {
            UserRegisteredEvent event = objectMapper.readValue(payload, UserRegisteredEvent.class);
            if (!"UserRegistered".equals(event.eventType())
                    || event.schemaVersion() != 1
                    || event.userId() == null) {
                throw new IllegalArgumentException("Unsupported identity event");
            }
            walletAccountService.provisionWallet(event.userId());
            log.info("Provisioned wallet for registered user {}", event.userId());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Identity event JSON is invalid", exception);
        }
    }
}
