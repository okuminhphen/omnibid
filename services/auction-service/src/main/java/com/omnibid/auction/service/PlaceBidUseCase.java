package com.omnibid.auction.service;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.Bid;
import com.omnibid.auction.dto.BidResponse;
import com.omnibid.auction.event.BidPlacedEvent;
import com.omnibid.auction.exception.BidConcurrencyException;
import com.omnibid.auction.exception.DomainException;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.repository.BidRepository;
import com.omnibid.auction.service.port.AuctionLockExecutor;
import com.omnibid.auction.service.port.AuctionPriceCache;
import com.omnibid.auction.service.port.WalletDepositPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class PlaceBidUseCase {
    private final AuctionLockExecutor lockExecutor;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final WalletDepositPort walletDepositPort;
    private final AuctionPriceCache priceCache;
    private final AuctionOutboxService outboxService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public BidResponse execute(UUID auctionId, UUID userId, BigDecimal bidAmount, String idempotencyKey) {
        validateInput(auctionId, userId, bidAmount, idempotencyKey);
        return lockExecutor.execute(auctionId, () -> placeWhileLocked(
                auctionId, userId, bidAmount, idempotencyKey.trim()
        ));
    }

    private BidResponse placeWhileLocked(
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount,
            String idempotencyKey
    ) {
        PlacementResult result;
        AtomicReference<ReservationContext> reservationRef = new AtomicReference<>();
        try {
            result = Objects.requireNonNull(transactionTemplate.execute(status -> placeInTransaction(
                    auctionId, userId, bidAmount, idempotencyKey, reservationRef
            )));
        } catch (RuntimeException exception) {
            compensateNewReservation(
                    reservationRef.get(), auctionId, userId, exception
            );
            if (exception instanceof OptimisticLockingFailureException) {
                throw new BidConcurrencyException(
                        "Phiên đấu giá vừa được cập nhật, vui lòng đặt giá lại!", exception
                );
            }
            throw exception;
        }
        if (result.created()) {
            priceCache.put(auctionId, bidAmount);
        }
        return result.response();
    }

    private PlacementResult placeInTransaction(
            UUID auctionId,
            UUID userId,
            BigDecimal bidAmount,
            String idempotencyKey,
            AtomicReference<ReservationContext> reservationRef
    ) {
        Bid duplicate = bidRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (duplicate != null) {
            validateDuplicate(duplicate, auctionId, userId, bidAmount);
            return new PlacementResult(BidResponse.from(duplicate), false);
        }

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new NoSuchElementException("Auction not found: " + auctionId));
        Instant now = clock.instant();
        auction.validateBid(bidAmount, now);

        WalletDepositPort.DepositReservation reservation = walletDepositPort.freeze(
                userId,
                auctionId,
                auction.getDepositAmount(),
                "deposit:" + auctionId + ":" + userId
        );
        reservationRef.set(new ReservationContext(reservation, auction.getDepositAmount()));
        if (!reservation.accepted()) {
            throw new DomainException(
                    "Không thể khóa tiền cọc: " + reservation.errorCode() + " - " + reservation.message()
            );
        }

        UUID bidId = UUID.randomUUID();
        auction.acceptBid(userId, bidAmount, now);
        auctionRepository.saveAndFlush(auction);

        Bid bid = Bid.place(
                bidId,
                auctionId,
                userId,
                bidAmount,
                idempotencyKey,
                reservation.transactionId(),
                now
        );
        bidRepository.save(bid);
        outboxService.enqueueBidPlaced(new BidPlacedEvent(
                bidId, auctionId, userId, bidAmount, now
        ));
        return new PlacementResult(BidResponse.from(bid), true);
    }

    private void compensateNewReservation(
            ReservationContext context,
            UUID auctionId,
            UUID userId,
            RuntimeException originalFailure
    ) {
        WalletDepositPort.DepositReservation reservation = context == null ? null : context.reservation();
        if (reservation == null || !reservation.accepted() || !reservation.newlyCreated()) {
            return;
        }
        try {
            walletDepositPort.release(
                    reservation.transactionId(),
                    userId,
                    auctionId,
                    context.amount(),
                    "release:" + reservation.transactionId()
            );
        } catch (RuntimeException compensationFailure) {
            // Preserve the command failure while retaining compensation evidence for logs/traces.
            originalFailure.addSuppressed(compensationFailure);
        }
    }

    private void validateInput(UUID auctionId, UUID userId, BigDecimal bidAmount, String idempotencyKey) {
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

    private void validateDuplicate(Bid duplicate, UUID auctionId, UUID userId, BigDecimal bidAmount) {
        boolean sameRequest = duplicate.getAuctionId().equals(auctionId)
                && duplicate.getBidderId().equals(userId)
                && duplicate.getAmount().compareTo(bidAmount) == 0;
        if (!sameRequest) {
            throw new DomainException("X-Idempotency-Key đã được dùng cho một lượt đặt giá khác");
        }
    }

    private record PlacementResult(BidResponse response, boolean created) {
    }

    private record ReservationContext(
            WalletDepositPort.DepositReservation reservation,
            BigDecimal amount
    ) {
    }
}
