package com.omnibid.auction.service;

import com.omnibid.auction.event.BidPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, BidPlacedEvent> kafkaTemplate;

    @Value("${omnibid.kafka.bid-topic}")
    private String bidTopic;

    public void sendBidEvent(BidPlacedEvent event) {
        try {
            kafkaTemplate.send(bidTopic, event.auctionId().toString(), event)
                    .get(5, TimeUnit.SECONDS);
            log.debug("Published bid event {}", event.bidId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publish was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not publish bid event " + event.bidId(), exception);
        }
    }
}
