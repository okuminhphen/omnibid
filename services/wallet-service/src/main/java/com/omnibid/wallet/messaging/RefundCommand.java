package com.omnibid.wallet.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundCommand(
        UUID transactionId,
        UUID userId,
        UUID auctionId,
        BigDecimal amount,
        int schemaVersion
) {
    public RefundCommand(UUID transactionId, UUID userId, UUID auctionId, BigDecimal amount) {
        this(transactionId, userId, auctionId, amount, 1);
    }

    public boolean hasSupportedSchema() {
        return schemaVersion == 0 || schemaVersion == 1;
    }
}
