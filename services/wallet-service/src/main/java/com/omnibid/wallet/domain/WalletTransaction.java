package com.omnibid.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_transaction_request", columnNames = "request_id")
})
@Getter
@Setter
@NoArgsConstructor
public class WalletTransaction {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false, length = 120)
    private String requestId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "auction_id")
    private UUID auctionId;

    @Column(name = "bid_id")
    private UUID bidId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletTransactionType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
