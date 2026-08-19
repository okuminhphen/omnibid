package com.omnibid.identity.service;

import java.time.Instant;

public record IssuedAccessToken(String value, Instant expiresAt) {
}
