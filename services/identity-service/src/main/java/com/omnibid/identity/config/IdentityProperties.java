package com.omnibid.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "omnibid.identity")
public record IdentityProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String refreshCookieName,
        boolean refreshCookieSecure,
        List<String> allowedOrigins,
        Google google,
        Kafka kafka
) {
    public record Google(boolean enabled, String clientId, String jwkSetUri) {
    }

    public record Kafka(String identityTopic) {
    }
}
