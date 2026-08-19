package com.omnibid.auction.service;

import com.omnibid.auction.event.BidPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, BidPlacedEvent> kafkaTemplate;

    @Value("${omnibid.kafka.bid-topic}")
    private String bidTopic;

    public void sendBidEvent(BidPlacedEvent event) {
        kafkaTemplate.send(bidTopic, event.auctionId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Could not publish bid event {}", event.bidId(), error);
                        return;
                    }
                    log.debug(
                            "Published bid event {} to partition {} at offset {}",
                            event.bidId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
