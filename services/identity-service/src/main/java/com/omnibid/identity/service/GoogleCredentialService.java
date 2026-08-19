package com.omnibid.identity.service;

import com.omnibid.identity.config.IdentityProperties;
import com.omnibid.identity.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class GoogleCredentialService {

    private final JwtDecoder googleJwtDecoder;
    private final IdentityProperties properties;

    public GoogleCredentialService(
            @Qualifier("googleJwtDecoder") JwtDecoder googleJwtDecoder,
            IdentityProperties properties
    ) {
        this.googleJwtDecoder = googleJwtDecoder;
        this.properties = properties;
    }

    public GooglePrincipal verify(String credential, String expectedNonce) {
        if (!properties.google().enabled() || properties.google().clientId().isBlank()) {
            throw new AuthenticationException("Google authentication is not configured");
        }

        try {
            Jwt jwt = googleJwtDecoder.decode(credential);
            String nonce = jwt.getClaimAsString("nonce");
            if (nonce == null || !nonce.equals(expectedNonce)) {
                throw new AuthenticationException("Google credential nonce is invalid");
            }

            Boolean emailVerified = jwt.getClaim("email_verified");
            if (!Boolean.TRUE.equals(emailVerified)) {
                throw new AuthenticationException("Google account email is not verified");
            }

            return new GooglePrincipal(
                    jwt.getSubject(),
                    requiredClaim(jwt, "email"),
                    jwt.getClaimAsString("name"),
                    jwt.getClaimAsString("picture"),
                    true
            );
        } catch (JwtException exception) {
            throw new AuthenticationException("Google credential is invalid", exception);
        }
    }

    private String requiredClaim(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null || value.isBlank()) {
            throw new AuthenticationException("Google credential is missing claim: " + claimName);
        }
        return value;
    }

    public record GooglePrincipal(
            String subject,
            String email,
            String displayName,
            String avatarUrl,
            boolean emailVerified
    ) {
    }
}
