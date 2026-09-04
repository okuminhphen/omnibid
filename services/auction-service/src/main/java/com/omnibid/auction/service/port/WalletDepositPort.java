package com.omnibid.auction.service.port;

import java.math.BigDecimal;
import java.util.UUID;

/** Outbound application port for the synchronous wallet reservation decision. */
public interface WalletDepositPort {
    DepositReservation freeze(UUID userId, UUID auctionId, BigDecimal amount, String idempotencyKey);

    void release(
            UUID transactionId,
            UUID userId,
            UUID auctionId,
            BigDecimal amount,
            String idempotencyKey
    );

    record DepositReservation(
            boolean accepted,
            UUID transactionId,
            boolean newlyCreated,
            String errorCode,
            String message
    ) {
        public static DepositReservation accepted(UUID transactionId, boolean newlyCreated) {
            if (transactionId == null) {
                throw new IllegalArgumentException("transactionId is required for an accepted reservation");
            }
            return new DepositReservation(true, transactionId, newlyCreated, "", "Deposit frozen");
        }

        public static DepositReservation rejected(String errorCode, String message) {
            return new DepositReservation(false, null, false, errorCode, message);
        }
    }
}
