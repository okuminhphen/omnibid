package com.omnibid.auction.infrastructure.cache;

import com.omnibid.auction.service.port.AuctionPriceCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAuctionPriceCache implements AuctionPriceCache {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void put(UUID auctionId, BigDecimal currentPrice) {
        try {
            redisTemplate.opsForValue().set(
                    "cache:auction:" + auctionId + ":price",
                    currentPrice.toPlainString()
            );
        } catch (RuntimeException exception) {
            // A cache outage must not turn a committed bid into an API failure.
            log.warn("Could not update price cache for auction {}", auctionId, exception);
        }
    }
}
