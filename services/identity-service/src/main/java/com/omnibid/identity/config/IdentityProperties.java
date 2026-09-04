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
        String refreshCookieSameSite,
        List<String> allowedOrigins,
        Google google,
        Admin admin,
        Kafka kafka
) {
    public IdentityProperties {
        refreshCookieSameSite = canonicalSameSite(refreshCookieSameSite);
        if ("None".equals(refreshCookieSameSite) && !refreshCookieSecure) {
            throw new IllegalArgumentException(
                    "SameSite=None refresh cookies require AUTH_COOKIE_SECURE=true"
            );
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }

    private static String canonicalSameSite(String value) {
        if (value == null) {
            return "Lax";
        }
        return switch (value.trim().toLowerCase()) {
            case "lax" -> "Lax";
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> throw new IllegalArgumentException(
                    "AUTH_COOKIE_SAME_SITE must be Lax, Strict, or None"
            );
        };
    }

    public record Google(boolean enabled, String clientId, String jwkSetUri) {
    }

    public record Admin(String email, String displayName) {
        public Admin {
            email = email == null ? "" : email.trim().toLowerCase();
            displayName = displayName == null || displayName.isBlank()
                    ? "OmniBid Administrator"
                    : displayName.trim();
        }

        public boolean configured() {
            return !email.isBlank();
        }
    }

    public record Kafka(String identityTopic) {
    }
}
