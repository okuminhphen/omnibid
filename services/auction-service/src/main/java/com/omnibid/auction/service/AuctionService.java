package com.omnibid.auction.service;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.AuctionStatus;
import com.omnibid.auction.domain.Bid;
import com.omnibid.auction.dto.BidResponse;
import com.omnibid.auction.dto.PlaceBidRequest;
import com.omnibid.auction.event.BidPlacedEvent;
import com.omnibid.auction.exception.DomainException;
import com.omnibid.auction.exception.LockUnavailableException;
import com.omnibid.auction.grpc.WalletGrpcClient;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.repository.BidRepository;
import com.omnibid.contract.wallet.v1.FreezeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final RedissonClient redissonClient;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final WalletGrpcClient walletGrpcClient;
    private final KafkaTemplate<String, BidPlacedEvent> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${omnibid.kafka.bid-topic}")
    private String bidTopic;

    @Value("${omnibid.lock.wait-seconds:2}")
    private long lockWaitSeconds;

    /**
     * Serializes bids for one auction across every service instance. Redisson's
     * watchdog keeps extending the lock while this process is alive because this
     * overload does not use a fixed lease time.
     */
    public BidResponse placeBid(UUID auctionId, PlaceBidRequest request, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);

        Bid existing = bidRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        String lockKey = "lock:auction:" + auctionId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(lockWaitSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                throw new LockUnavailableException("Auction is processing another bid; please retry");
            }

            PlacementResult result = Objects.requireNonNull(
                    transactionTemplate.execute(status -> placeBidInTransaction(
                            auctionId, request, idempotencyKey
                    ))
            );

            if (result.event() != null) {
                publishBidEvent(result.event());
            }
            return result.response();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LockUnavailableException("Interrupted while waiting for the auction lock");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private PlacementResult placeBidInTransaction(
            UUID auctionId,
            PlaceBidRequest request,
            String idempotencyKey
    ) {
        Bid duplicate = bidRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (duplicate != null) {
            return new PlacementResult(toResponse(duplicate), null);
        }

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new NoSuchElementException("Auction not found: " + auctionId));

        Instant now = Instant.now();
        if (auction.getStatus() != AuctionStatus.ACTIVE || !auction.getEndsAt().isAfter(now)) {
            throw new DomainException("Auction is not active");
        }
        if (request.amount().compareTo(auction.getCurrentPrice()) <= 0) {
            throw new DomainException("Bid must be greater than current price " + auction.getCurrentPrice());
        }

        UUID bidId = UUID.randomUUID();
        FreezeResponse freeze = walletGrpcClient.freezeDeposit(
                idempotencyKey,
                auctionId,
                request.bidderId(),
                bidId,
                request.amount()
        );
        if (!freeze.getSuccess()) {
            throw new DomainException("Wallet rejected deposit: " + freeze.getErrorCode() + " - " + freeze.getMessage());
        }

        UUID walletTransactionId = UUID.fromString(freeze.getTransactionId());
        auction.setCurrentPrice(request.amount());
        auction.setHighestBidderId(request.bidderId());
        auctionRepository.save(auction);

        Bid bid = new Bid();
        bid.setId(bidId);
        bid.setAuctionId(auctionId);
        bid.setBidderId(request.bidderId());
        bid.setAmount(request.amount());
        bid.setIdempotencyKey(idempotencyKey);
        bid.setWalletTransactionId(walletTransactionId);
        bid.setPlacedAt(now);
        bidRepository.save(bid);

        BidPlacedEvent event = new BidPlacedEvent(
                UUID.randomUUID(),
                1,
                bidId,
                auctionId,
                request.bidderId(),
                request.amount(),
                walletTransactionId,
                idempotencyKey,
                now
        );
        return new PlacementResult(toResponse(bid), event);
    }

    private void publishBidEvent(BidPlacedEvent event) {
        kafkaTemplate.send(bidTopic, event.auctionId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Could not publish bid event {}. Use an outbox before production.", event.eventId(), error);
                    }
                });
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new IllegalArgumentException("X-Idempotency-Key must contain 1-100 characters");
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
