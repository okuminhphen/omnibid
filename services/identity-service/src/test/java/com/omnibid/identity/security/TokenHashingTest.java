package com.omnibid.identity.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHashingTest {

    private final TokenHashing tokenHashing = new TokenHashing();

    @Test
    void createsHighEntropyUrlSafeTokensAndStoresOnlyDeterministicHashes() {
        String first = tokenHashing.newOpaqueToken();
        String second = tokenHashing.newOpaqueToken();

        assertThat(first).hasSize(43).doesNotContain("=", "+", "/");
        assertThat(second).isNotEqualTo(first);
        assertThat(tokenHashing.sha256(first)).hasSize(64).isNotEqualTo(first);
        assertThat(tokenHashing.matches(first, tokenHashing.sha256(first))).isTrue();
        assertThat(tokenHashing.matches(second, tokenHashing.sha256(first))).isFalse();
    }
}
