package com.omnibid.auction.infrastructure.lock;

import com.omnibid.auction.exception.BidConcurrencyException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonAuctionLockExecutorTest {
    @Test
    void executesAndReleasesOnlyWhenLockIsOwned() throws InterruptedException {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        UUID auctionId = UUID.randomUUID();
        when(redisson.getLock("lock:auction:" + auctionId)).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        String result = new RedissonAuctionLockExecutor(redisson).execute(auctionId, () -> "done");

        assertThat(result).isEqualTo("done");
        verify(lock).unlock();
    }

    @Test
    void rejectsWhenLockCannotBeAcquired() throws InterruptedException {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        UUID auctionId = UUID.randomUUID();
        when(redisson.getLock("lock:auction:" + auctionId)).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> new RedissonAuctionLockExecutor(redisson)
                .execute(auctionId, () -> "never"))
                .isInstanceOf(BidConcurrencyException.class);
        verify(lock, never()).unlock();
    }
}
