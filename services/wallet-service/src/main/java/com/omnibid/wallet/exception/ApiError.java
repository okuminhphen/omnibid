package com.omnibid.wallet.exception;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp) {
}
