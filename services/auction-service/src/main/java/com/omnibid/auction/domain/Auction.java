package com.omnibid.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auctions")
@Getter
@NoArgsConstructor
public class Auction {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "starting_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "step_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal stepPrice;

    @Column(name = "deposit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "winning_user_id")
    private UUID winningUserId;

    @Version
    private long version;

    public static Auction schedule(
            UUID id,
            String title,
            BigDecimal startingPrice,
            BigDecimal stepPrice,
            BigDecimal depositAmount,
            Instant startTime,
            Instant endTime
    ) {
        if (id == null) {
            throw new IllegalArgumentException("Auction id is required");
        }
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new IllegalArgumentException("Auction title must contain 1-200 characters");
        }
        requirePositive(startingPrice, "startingPrice", true);
        requirePositive(stepPrice, "stepPrice", false);
        requirePositive(depositAmount, "depositAmount", false);
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Auction endTime must be after startTime");
        }

        Auction auction = new Auction();
        auction.id = id;
        auction.title = title.trim();
        auction.startingPrice = startingPrice;
        auction.currentPrice = startingPrice;
        auction.stepPrice = stepPrice;
        auction.depositAmount = depositAmount;
        auction.status = AuctionStatus.PENDING;
        auction.startTime = startTime;
        auction.endTime = endTime;
        return auction;
    }

    public void activate(Instant now) {
        if (status != AuctionStatus.PENDING) {
            throw new IllegalStateException("Only a pending auction can be activated");
        }
        if (now == null || now.isBefore(startTime) || !now.isBefore(endTime)) {
            throw new IllegalStateException("Auction cannot be activated outside its time window");
        }
        status = AuctionStatus.ACTIVE;
    }

    public boolean isActiveAt(Instant time) {
        return status == AuctionStatus.ACTIVE
                && !time.isBefore(startTime)
                && time.isBefore(endTime);
    }

    public BigDecimal minimumNextBid() {
        return currentPrice.add(stepPrice);
    }

    public void validateBid(BigDecimal bidAmount, Instant now) {
        if (!isActiveAt(now)) {
            throw new com.omnibid.auction.exception.DomainException(
                    "Phiên đấu giá không ở trạng thái ACTIVE"
            );
        }
        if (bidAmount == null || bidAmount.compareTo(minimumNextBid()) < 0) {
            throw new com.omnibid.auction.exception.DomainException(
                    "Giá đặt tối thiểu là " + minimumNextBid().toPlainString()
            );
        }
    }

    public void acceptBid(UUID userId, BigDecimal bidAmount, Instant now) {
        if (userId == null) {
            throw new IllegalArgumentException("Bidder id is required");
        }
        validateBid(bidAmount, now);
        currentPrice = bidAmount;
        winningUserId = userId;
    }

    public void end(Instant endedAt) {
        if (status == AuctionStatus.PENDING) {
            throw new IllegalStateException("A pending auction cannot be ended");
        }
        if (endedAt == null) {
            throw new IllegalArgumentException("endedAt is required");
        }
        status = AuctionStatus.ENDED;
        endTime = endedAt;
    }

    private static void requirePositive(BigDecimal value, String field, boolean zeroAllowed) {
        if (value == null || (zeroAllowed ? value.signum() < 0 : value.signum() <= 0)) {
            throw new IllegalArgumentException(field + (zeroAllowed ? " must not be negative" : " must be positive"));
        }
    }
}
