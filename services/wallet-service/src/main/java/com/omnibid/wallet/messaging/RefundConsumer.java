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

        if (isCachedAsSuccessful(key)) {
            log.info("Skipping duplicate refund transaction {}", command.transactionId());
            return;
        }

        // The PostgreSQL transaction and unique refund:{transactionId} key are the
        // correctness boundary. A PROCESSING Redis marker is deliberately not used:
        // acknowledging a redelivery against a stale marker could lose a refund after
        // a consumer crash. Redis only caches a result after the database commit.
        refundService.refund(command);
        cacheSuccessfulResult(key, command);
    }

    private boolean isCachedAsSuccessful(String key) {
        try {
            return "SUCCESS".equals(redisTemplate.opsForValue().get(key));
        } catch (RuntimeException exception) {
            log.warn("Redis refund dedupe lookup failed for key {}; using PostgreSQL", key, exception);
            return false;
        }
    }

    private void cacheSuccessfulResult(String key, RefundCommand command) {
        try {
            redisTemplate.opsForValue().set(key, "SUCCESS", 30, TimeUnit.DAYS);
        } catch (RuntimeException exception) {
            log.warn(
                    "Refund committed but Redis success marker could not be stored for transaction {}",
                    command.transactionId(),
                    exception
            );
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
