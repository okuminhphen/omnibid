package com.omnibid.wallet.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundMessage(
        String messageId,
        UUID walletId,
        UUID auctionId,
        UUID freezeTransactionId,
        BigDecimal amount
) {
}
