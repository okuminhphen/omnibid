package com.omnibid.wallet.messaging;

import com.omnibid.wallet.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundConsumer {

    private static final Duration PROCESSING_TTL = Duration.ofMinutes(5);
    private static final Duration COMPLETED_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;
    private final RefundService refundService;

    @RabbitListener(queues = "${omnibid.rabbitmq.refund-queue}")
    public void consume(RefundMessage message) {
        validate(message);
        String key = "idempotent:refund:" + message.messageId();

        String currentState = redisTemplate.opsForValue().get(key);
        if ("COMPLETED".equals(currentState)) {
            log.info("Skipping completed refund message {}", message.messageId());
            return;
        }

        Boolean reserved = redisTemplate.opsForValue()
                .setIfAbsent(key, "PROCESSING", PROCESSING_TTL);
        if (!Boolean.TRUE.equals(reserved)) {
            // A prior process may have died after SETNX but before DB commit. Continue
            // through the durable DB idempotency check instead of ACKing and losing it.
            log.warn("Refund {} already has PROCESSING marker; verifying durable state", message.messageId());
        }

        try {
            // The DB transaction also has a unique request_id as a second safety net
            // if Redis loses data or the marker expires after a process crash.
            refundService.refund(message);
            redisTemplate.opsForValue().set(key, "COMPLETED", COMPLETED_TTL);
        } catch (RuntimeException exception) {
            if (Boolean.TRUE.equals(reserved)) {
                redisTemplate.delete(key);
            }
            throw exception;
        }
    }

    private void validate(RefundMessage message) {
        if (message == null || message.messageId() == null || message.messageId().isBlank()
                || message.messageId().length() > 100 || message.walletId() == null) {
            throw new IllegalArgumentException("Invalid refund message");
        }
    }
}
