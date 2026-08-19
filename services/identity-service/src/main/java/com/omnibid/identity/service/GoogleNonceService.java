package com.omnibid.identity.service;

import com.omnibid.identity.exception.AuthenticationException;
import com.omnibid.identity.security.TokenHashing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleNonceService {

    private final TokenHashing tokenHashing;

    public Nonce issue() {
        String value = tokenHashing.newOpaqueToken();
        return new Nonce(value, tokenHashing.sha256(value));
    }

    public void validate(String nonce, String expectedHash) {
        if (nonce == null
                || nonce.isBlank()
                || expectedHash == null
                || expectedHash.isBlank()
                || !tokenHashing.matches(nonce, expectedHash)) {
            throw new AuthenticationException("Google login nonce is missing or invalid");
        }
    }

    public record Nonce(String value, String hash) {
    }
}
