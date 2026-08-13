package com.omnibid.auction.service;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.AuctionStatus;
import com.omnibid.auction.domain.Bid;
import com.omnibid.auction.dto.BidResponse;
import com.omnibid.auction.dto.EndAuctionResponse;
import com.omnibid.auction.event.BidPlacedEvent;
import com.omnibid.auction.exception.BidConcurrencyException;
import com.omnibid.auction.exception.DomainException;
import com.omnibid.auction.grpc.WalletClient;
import com.omnibid.auction.messaging.RefundCommand;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.repository.BidRepository;
import com.omnibid.contract.wallet.v1.FreezeDepositResponse;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {

    private static final long LOCK_WAIT_SECONDS = 3;
    private static final long LOCK_LEASE_SECONDS = 5;

    private final RedissonClient redissonClient;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final WalletClient walletClient;
    private final StringRedisTemplate redisTemplate;
    private final KafkaProducerService kafkaProducerService;
    private final RabbitMQPublisherService rabbitMQPublisherService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public BidResponse placeBid(
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount,
            String idempotencyKey
    ) {
        validateInput(auctionId, userId, bidAmount, idempotencyKey);

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
                                bidAmount,
                                idempotencyKey
                        ))
                );
            } catch (OptimisticLockingFailureException exception) {
                // @Version is the final correctness guard if another writer bypasses Redis.
                throw new BidConcurrencyException(
                        "Phiên đấu giá vừa được cập nhật, vui lòng đặt giá lại!",
                        exception
                );
            }

            if (result.event() != null) {
                String priceCacheKey = "cache:auction:" + auctionId + ":price";
                redisTemplate.opsForValue().set(priceCacheKey, bidAmount.toPlainString());
                kafkaProducerService.sendBidEvent(result.event());
            }

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

    @Override
    public EndAuctionResponse endAuction(UUID auctionId) {
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId là bắt buộc");
        }

        String lockKey = "lock:auction:" + auctionId;
        RLock lock = redissonClient.getLock(lockKey);
        EndResult result;

        try {
            boolean acquired = lock.tryLock(
                    LOCK_WAIT_SECONDS,
                    LOCK_LEASE_SECONDS,
                    TimeUnit.SECONDS
            );
            if (!acquired) {
                throw new BidConcurrencyException("Hệ thống đang quá tải, vui lòng thử lại!");
            }

            result = Objects.requireNonNull(
                    transactionTemplate.execute(status -> endAuctionInTransaction(auctionId))
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BidConcurrencyException("Hệ thống đang quá tải, vui lòng thử lại!", exception);
        } catch (OptimisticLockingFailureException exception) {
            throw new BidConcurrencyException(
                    "Phiên đấu giá vừa được cập nhật, vui lòng thử kết thúc lại!",
                    exception
            );
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        // Publish after the auction transaction commits. Repeating /end is safe:
        // it republishes the same transactionIds and the wallet consumer deduplicates them.
        result.refundCommands().forEach(rabbitMQPublisherService::sendRefundCommand);
        return new EndAuctionResponse(
                result.auctionId(),
                result.status(),
                result.winningUserId(),
                result.refundCommands().size()
        );
    }

    private PlacementResult placeBidInTransaction(
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount,
            String idempotencyKey
    ) {
        Bid duplicate = bidRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (duplicate != null) {
            validateDuplicateBid(duplicate, auctionId, userId, bidAmount);
            return new PlacementResult(toResponse(duplicate), null);
        }

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
        bid.setIdempotencyKey(idempotencyKey);
        bid.setWalletTransactionId(walletTransactionId);
        bid.setPlacedAt(now);
        bidRepository.save(bid);

        BidPlacedEvent event = new BidPlacedEvent(
                bidId,
                auctionId,
                userId,
                bidAmount,
                now
        );
        return new PlacementResult(toResponse(bid), event);
    }

    private EndResult endAuctionInTransaction(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new NoSuchElementException("Auction not found: " + auctionId));

        if (auction.getStatus() == AuctionStatus.PENDING) {
            throw new DomainException("Không thể kết thúc phiên đấu giá đang PENDING");
        }
        if (auction.getStatus() != AuctionStatus.ENDED) {
            auction.end(Instant.now());
            auctionRepository.saveAndFlush(auction);
        }

        Map<UUID, RefundCommand> losingUsers = new LinkedHashMap<>();
        for (Bid bid : bidRepository.findAllByAuctionIdOrderByPlacedAtAsc(auctionId)) {
            if (Objects.equals(bid.getBidderId(), auction.getWinningUserId())) {
                continue;
            }
            losingUsers.putIfAbsent(
                    bid.getBidderId(),
                    new RefundCommand(
                            bid.getWalletTransactionId(),
                            bid.getBidderId(),
                            auctionId,
                            auction.getDepositAmount()
                    )
            );
        }

        return new EndResult(
                auction.getId(),
                auction.getStatus(),
                auction.getWinningUserId(),
                List.copyOf(losingUsers.values())
        );
    }

    private void validateInput(
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount,
            String idempotencyKey
    ) {
        if (auctionId == null || userId == null) {
            throw new IllegalArgumentException("auctionId và userId là bắt buộc");
        }
        if (bidAmount == null || bidAmount.signum() <= 0) {
            throw new IllegalArgumentException("bidAmount phải lớn hơn 0");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("X-Idempotency-Key phải có từ 1 đến 100 ký tự");
        }
    }

    private void validateDuplicateBid(
            Bid duplicate,
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount
    ) {
        boolean sameRequest = duplicate.getAuctionId().equals(auctionId)
                && duplicate.getBidderId().equals(userId)
                && duplicate.getAmount().compareTo(bidAmount) == 0;
        if (!sameRequest) {
            throw new DomainException("X-Idempotency-Key đã được dùng cho một lượt đặt giá khác");
        }
    }

    private BidResponse toResponse(Bid bid) {
        return BidResponse.from(bid);
    }

    private record PlacementResult(BidResponse response, BidPlacedEvent event) {
    }

    private record EndResult(
            UUID auctionId,
            AuctionStatus status,
            UUID winningUserId,
            List<RefundCommand> refundCommands
    ) {
    }
}
