package com.omnibid.identity.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class IdentityPropertiesTest {

    @Test
    void canonicalizesCrossSiteCookieConfiguration() {
        IdentityProperties properties = properties(true, "none");

        assertThat(properties.refreshCookieSameSite()).isEqualTo("None");
    }

    @Test
    void rejectsInsecureSameSiteNoneCookie() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties(false, "None"))
                .withMessageContaining("AUTH_COOKIE_SECURE=true");
    }

    private IdentityProperties properties(boolean secure, String sameSite) {
        return new IdentityProperties(
                "https://identity.example.com",
                "omnibid-api",
                Duration.ofMinutes(10),
                Duration.ofDays(30),
                "omnibid_refresh",
                secure,
                sameSite,
                List.of("https://omnibid.vercel.app"),
                new IdentityProperties.Google(false, "", "https://example.com/jwks"),
                new IdentityProperties.Kafka("identity-events")
        );
    }
}
