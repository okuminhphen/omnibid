package com.omnibid.wallet.messaging;

import com.omnibid.wallet.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundConsumer {

    private final StringRedisTemplate redisTemplate;
    private final RefundService refundService;

    @RabbitListener(queues = "${omnibid.rabbitmq.refund-queue}")
    public void consume(RefundCommand command) {
        validate(command);
        String key = "idempotent:refund:" + command.transactionId();

        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(
                key,
                "PROCESSING",
                10,
                TimeUnit.MINUTES
        );
        if (!Boolean.TRUE.equals(reserved)) {
            log.info("Skipping duplicate refund transaction {}", command.transactionId());
            return;
        }

        try {
            // PostgreSQL also stores a unique refund:{transactionId} key. Redis is the
            // fast dedupe layer; the database remains the durable safety net.
            refundService.refund(command);
            redisTemplate.opsForValue().set(key, "SUCCESS", 30, TimeUnit.DAYS);
        } catch (RuntimeException exception) {
            redisTemplate.delete(key);
            throw exception;
        }
    }

    private void validate(RefundCommand command) {
        if (command == null
                || command.transactionId() == null
                || command.userId() == null
                || command.auctionId() == null
                || command.amount() == null
                || command.amount().signum() <= 0
                || !command.hasSupportedSchema()) {
            throw new IllegalArgumentException("Invalid refund command");
        }
    }
}
