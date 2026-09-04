package com.omnibid.wallet.messaging;

import com.omnibid.wallet.service.RefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundConsumerTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RefundService refundService;

    @InjectMocks
    private RefundConsumer consumer;

    private RefundCommand command;
    private String key;

    @BeforeEach
    void setUp() {
        command = new RefundCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00")
        );
        key = "idempotent:refund:" + command.transactionId();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void refundsThenCachesSuccess() {
        when(valueOperations.get(key)).thenReturn(null);

        consumer.consume(command);

        verify(refundService).refund(command);
        verify(valueOperations).set(key, "SUCCESS", 30, TimeUnit.DAYS);
    }

    @Test
    void acknowledgesDuplicateWithoutRefundingAgain() {
        when(valueOperations.get(key)).thenReturn("SUCCESS");

        consumer.consume(command);

        verify(refundService, never()).refund(command);
    }

    @Test
    void staleProcessingMarkerCannotDropARedeliveredRefund() {
        when(valueOperations.get(key)).thenReturn("PROCESSING");

        consumer.consume(command);

        verify(refundService).refund(command);
        verify(valueOperations).set(key, "SUCCESS", 30, TimeUnit.DAYS);
    }

    @Test
    void redisReadFailureFallsBackToDurableDatabaseIdempotency() {
        when(valueOperations.get(key)).thenThrow(new IllegalStateException("Redis unavailable"));

        consumer.consume(command);

        verify(refundService).refund(command);
    }

    @Test
    void redisWriteFailureDoesNotFailACommittedRefund() {
        when(valueOperations.get(key)).thenReturn(null);
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(valueOperations).set(key, "SUCCESS", 30, TimeUnit.DAYS);

        assertDoesNotThrow(() -> consumer.consume(command));
        verify(refundService).refund(command);
    }
}
