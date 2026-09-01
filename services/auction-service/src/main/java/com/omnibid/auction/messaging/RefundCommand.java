package com.omnibid.auction.messaging;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record RefundCommand(
        UUID transactionId,
        UUID userId,
        UUID auctionId,
        BigDecimal amount,
        int schemaVersion
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public RefundCommand(UUID transactionId, UUID userId, UUID auctionId, BigDecimal amount) {
        this(transactionId, userId, auctionId, amount, 1);
    }
}
