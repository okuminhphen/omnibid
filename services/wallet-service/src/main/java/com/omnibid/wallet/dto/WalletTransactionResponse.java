package com.omnibid.wallet.dto;

import com.omnibid.wallet.domain.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        UUID auctionId,
        BigDecimal amount,
        String type,
        String status,
        Instant createdAt
) {
    public static WalletTransactionResponse from(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getAuctionId(),
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
