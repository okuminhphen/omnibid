package com.omnibid.wallet.service;

import java.util.UUID;

public record FreezeResult(
        boolean success,
        UUID transactionId,
        boolean newlyCreated,
        String errorCode,
        String message
) {
    public static FreezeResult success(UUID transactionId, boolean newlyCreated) {
        return new FreezeResult(true, transactionId, newlyCreated, "", "Deposit frozen");
    }

    public static FreezeResult rejected(String errorCode, String message) {
        return new FreezeResult(false, null, false, errorCode, message);
    }
}
