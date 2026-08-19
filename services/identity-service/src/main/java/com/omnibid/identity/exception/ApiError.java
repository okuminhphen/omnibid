package com.omnibid.identity.exception;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String code, String message) {
}
