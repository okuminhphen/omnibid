package com.omnibid.auction.service;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.Bid;
import com.omnibid.auction.dto.BidResponse;
import com.omnibid.auction.event.BidPlacedEvent;
import com.omnibid.auction.exception.BidConcurrencyException;
import com.omnibid.auction.exception.DomainException;
import com.omnibid.auction.grpc.WalletClient;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.repository.BidRepository;
import com.omnibid.contract.wallet.v1.FreezeDepositResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionServiceImpl implements AuctionService {

    private static final long LOCK_WAIT_SECONDS = 3;
    private static final long LOCK_LEASE_SECONDS = 5;

    private final RedissonClient redissonClient;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final WalletClient walletClient;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, BidPlacedEvent> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${omnibid.kafka.bid-topic}")
    private String bidTopic;

    @Override
    public BidResponse placeBid(UUID auctionId, UUID userId, BigDecimal bidAmount) {
        validateInput(auctionId, userId, bidAmount);

        String lockKey = "lock:auction:" + auctionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                    LOCK_WAIT_SECONDS,
                    LOCK_LEASE_SECONDS,
                    TimeUnit.SECONDS
            );
            if (!acquired) {
                throw new BidConcurrencyException("Hệ thống đang quá tải, vui lòng thử lại!");
            }

            PlacementResult result;
            try {
                result = Objects.requireNonNull(
                        transactionTemplate.execute(status -> placeBidInTransaction(
                                auctionId,
                                userId,
                                bidAmount
                        ))
                );
            } catch (OptimisticLockingFailureException exception) {
                // @Version is the final correctness guard if another writer bypasses Redis.
                throw new BidConcurrencyException(
                        "Phiên đấu giá vừa được cập nhật, vui lòng đặt giá lại!",
                        exception
                );
            }

            String priceCacheKey = "cache:auction:" + auctionId + ":price";
            redisTemplate.opsForValue().set(priceCacheKey, bidAmount.toPlainString());
            publishBidEvent(result.event());

            return result.response();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BidConcurrencyException("Hệ thống đang quá tải, vui lòng thử lại!", exception);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private PlacementResult placeBidInTransaction(
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount
    ) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new NoSuchElementException("Auction not found: " + auctionId));

        Instant now = Instant.now();
        if (!auction.isActiveAt(now)) {
            throw new DomainException("Phiên đấu giá không ở trạng thái ACTIVE");
        }

        BigDecimal minimumBid = auction.minimumNextBid();
        if (bidAmount.compareTo(minimumBid) < 0) {
            throw new DomainException("Giá đặt tối thiểu là " + minimumBid.toPlainString());
        }

        UUID bidId = UUID.randomUUID();
        String depositIdempotencyKey = "deposit:" + auctionId + ":" + userId;
        FreezeDepositResponse freezeResponse = walletClient.freezeDeposit(
                userId,
                auction.getDepositAmount(),
                auctionId,
                depositIdempotencyKey
        );
        if (!freezeResponse.getSuccess()) {
            throw new DomainException(
                    "Không thể khóa tiền cọc: "
                            + freezeResponse.getErrorCode()
                            + " - "
                            + freezeResponse.getMessage()
            );
        }
        if (freezeResponse.getTransactionId().isBlank()) {
            throw new DomainException("Wallet service không trả về transactionId");
        }

        UUID walletTransactionId = UUID.fromString(freezeResponse.getTransactionId());
        auction.acceptBid(userId, bidAmount);
        // saveAndFlush forces the @Version check before the Redis lock is released.
        auctionRepository.saveAndFlush(auction);

        Bid bid = new Bid();
        bid.setId(bidId);
        bid.setAuctionId(auctionId);
        bid.setBidderId(userId);
        bid.setAmount(bidAmount);
        bid.setIdempotencyKey("bid:" + bidId);
        bid.setWalletTransactionId(walletTransactionId);
        bid.setPlacedAt(now);
        bidRepository.save(bid);

        BidPlacedEvent event = new BidPlacedEvent(
                UUID.randomUUID(),
                1,
                bidId,
                auctionId,
                userId,
                bidAmount,
                walletTransactionId,
                depositIdempotencyKey,
                now
        );
        return new PlacementResult(toResponse(bid), event);
    }

    private void publishBidEvent(BidPlacedEvent event) {
        kafkaTemplate.send(bidTopic, event.auctionId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Could not publish bid event {}", event.eventId(), error);
                    }
                });
    }

    private void validateInput(UUID auctionId, UUID userId, BigDecimal bidAmount) {
        if (auctionId == null || userId == null) {
            throw new IllegalArgumentException("auctionId và userId là bắt buộc");
        }
        if (bidAmount == null || bidAmount.signum() <= 0) {
            throw new IllegalArgumentException("bidAmount phải lớn hơn 0");
        }
    }

    private BidResponse toResponse(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuctionId(),
                bid.getBidderId(),
                bid.getAmount(),
                bid.getWalletTransactionId(),
                bid.getPlacedAt()
        );
    }

    private record PlacementResult(BidResponse response, BidPlacedEvent event) {
    }
}
