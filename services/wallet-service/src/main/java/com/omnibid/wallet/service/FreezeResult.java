package com.omnibid.wallet.service;

import java.util.UUID;

public record FreezeResult(
        boolean success,
        UUID transactionId,
        String errorCode,
        String message
) {
    public static FreezeResult success(UUID transactionId) {
        return new FreezeResult(true, transactionId, "", "Deposit frozen");
    }

    public static FreezeResult rejected(String errorCode, String message) {
        return new FreezeResult(false, null, errorCode, message);
    }
}
