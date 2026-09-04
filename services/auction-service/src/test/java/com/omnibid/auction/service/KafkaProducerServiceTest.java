package com.omnibid.auction.service;

import com.omnibid.auction.event.BidPlacedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, BidPlacedEvent> kafkaTemplate;

    @InjectMocks
    private KafkaProducerService service;

    @Test
    void sendsBidEventUsingAuctionIdAsPartitionKey() {
        UUID auctionId = UUID.randomUUID();
        BidPlacedEvent event = new BidPlacedEvent(
                UUID.randomUUID(),
                auctionId,
                UUID.randomUUID(),
                new BigDecimal("250.00"),
                Instant.now()
        );
        @SuppressWarnings("unchecked")
        SendResult<String, BidPlacedEvent> result = org.mockito.Mockito.mock(SendResult.class);
        ReflectionTestUtils.setField(service, "bidTopic", "bid-events");
        when(kafkaTemplate.send("bid-events", auctionId.toString(), event))
                .thenReturn(CompletableFuture.completedFuture(result));

        service.sendBidEvent(event);

        verify(kafkaTemplate).send("bid-events", auctionId.toString(), event);
    }
}
