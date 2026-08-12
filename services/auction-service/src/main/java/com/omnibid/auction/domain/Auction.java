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
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auctions")
@Getter
@Setter
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

    public boolean isActiveAt(Instant time) {
        return status == AuctionStatus.ACTIVE
                && !time.isBefore(startTime)
                && time.isBefore(endTime);
    }

    public BigDecimal minimumNextBid() {
        return currentPrice.add(stepPrice);
    }

    public void acceptBid(UUID userId, BigDecimal bidAmount) {
        currentPrice = bidAmount;
        winningUserId = userId;
    }
}
