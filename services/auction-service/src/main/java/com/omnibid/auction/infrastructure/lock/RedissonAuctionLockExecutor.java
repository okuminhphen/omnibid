package com.omnibid.auction.infrastructure.lock;

import com.omnibid.auction.exception.BidConcurrencyException;
import com.omnibid.auction.service.port.AuctionLockExecutor;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RedissonAuctionLockExecutor implements AuctionLockExecutor {
    private static final long WAIT_SECONDS = 3;
    private static final String LOCK_PREFIX = "lock:auction:";

    private final RedissonClient redissonClient;

    @Override
    public <T> T execute(UUID auctionId, Supplier<T> action) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + auctionId);
        try {
            // No fixed lease: Redisson's watchdog renews the lock while this instance is alive.
            if (!lock.tryLock(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new BidConcurrencyException("Hệ thống đang quá tải, vui lòng thử lại!");
            }
            return action.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BidConcurrencyException("Hệ thống đang quá tải, vui lòng thử lại!", exception);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
