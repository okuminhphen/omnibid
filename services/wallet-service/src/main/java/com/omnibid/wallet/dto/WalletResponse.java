package com.omnibid.wallet.dto;

import com.omnibid.wallet.domain.Wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        BigDecimal frozenBalance,
        BigDecimal availableBalance,
        Instant updatedAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getFrozenBalance(),
                wallet.getAvailableBalance(),
                wallet.getUpdatedAt()
        );
    }
}
