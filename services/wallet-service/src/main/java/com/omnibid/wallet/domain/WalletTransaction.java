package com.omnibid.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "uk_wallet_transaction_idempotency", columnList = "idempotency_key", unique = true)
})
@Getter
@NoArgsConstructor
public class WalletTransaction {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "auction_id")
    private UUID auctionId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletTransactionStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static WalletTransaction succeeded(
            UUID id,
            UUID walletId,
            UUID auctionId,
            BigDecimal amount,
            WalletTransactionType type,
            String idempotencyKey
    ) {
        if (id == null || walletId == null || type == null) {
            throw new IllegalArgumentException("Wallet transaction identifiers and type are required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Wallet transaction amount must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) {
            throw new IllegalArgumentException("Wallet transaction idempotency key must contain 1-120 characters");
        }
        WalletTransaction transaction = new WalletTransaction();
        transaction.id = id;
        transaction.walletId = walletId;
        transaction.auctionId = auctionId;
        transaction.amount = amount;
        transaction.type = type;
        transaction.status = WalletTransactionStatus.SUCCESS;
        transaction.idempotencyKey = idempotencyKey;
        return transaction;
    }
}
