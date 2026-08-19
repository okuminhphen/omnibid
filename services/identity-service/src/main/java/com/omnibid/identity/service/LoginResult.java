package com.omnibid.identity.service;

public record LoginResult(
        IssuedAccessToken accessToken,
        String refreshToken,
        AuthenticatedUser user
) {
}
