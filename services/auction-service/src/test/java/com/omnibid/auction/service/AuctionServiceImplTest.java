package com.omnibid.auction.service;

import com.omnibid.auction.exception.BidConcurrencyException;
import com.omnibid.auction.grpc.WalletClient;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.repository.BidRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionServiceImplTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private WalletClient walletClient;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AuctionOutboxService auctionOutboxService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private RLock lock;

    @InjectMocks
    private AuctionServiceImpl service;

    @Test
    void rejectsBidWhenDistributedLockCannotBeAcquired() throws InterruptedException {
        UUID auctionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String lockKey = "lock:auction:" + auctionId;

        when(redissonClient.getLock(lockKey)).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> service.placeBid(
                auctionId,
                userId,
                new BigDecimal("120.00"),
                UUID.randomUUID().toString()
        ))
                .isInstanceOf(BidConcurrencyException.class)
                .hasMessage("Hệ thống đang quá tải, vui lòng thử lại!");

        verify(lock).tryLock(3, TimeUnit.SECONDS);
        verify(lock, never()).unlock();
    }
}
