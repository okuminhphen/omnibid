package com.omnibid.auction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bids", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bid_idempotency_key", columnNames = "idempotency_key")
})
@Getter
@NoArgsConstructor
public class Bid {

    @Id
    private UUID id;

    @Column(name = "auction_id", nullable = false)
    private UUID auctionId;

    @Column(name = "bidder_id", nullable = false)
    private UUID bidderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "wallet_transaction_id", nullable = false)
    private UUID walletTransactionId;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    public static Bid place(
            UUID id,
            UUID auctionId,
            UUID bidderId,
            BigDecimal amount,
            String idempotencyKey,
            UUID walletTransactionId,
            Instant placedAt
    ) {
        if (id == null || auctionId == null || bidderId == null || walletTransactionId == null) {
            throw new IllegalArgumentException("Bid identifiers are required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Bid amount must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("Bid idempotency key must contain 1-100 characters");
        }
        if (placedAt == null) {
            throw new IllegalArgumentException("Bid placedAt is required");
        }

        Bid bid = new Bid();
        bid.id = id;
        bid.auctionId = auctionId;
        bid.bidderId = bidderId;
        bid.amount = amount;
        bid.idempotencyKey = idempotencyKey;
        bid.walletTransactionId = walletTransactionId;
        bid.placedAt = placedAt;
        return bid;
    }
}
