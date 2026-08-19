package com.omnibid.wallet.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundCommand(
        UUID transactionId,
        UUID userId,
        UUID auctionId,
        BigDecimal amount
) {
}
