package com.omnibid.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;

@Configuration
public class JwtConfig {

    @Bean
    RSAKey omniBidRsaKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey key) {
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(key));
        return new NimbusJwtEncoder(source);
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder(RSAKey key, IdentityProperties properties) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.issuer()),
                audienceValidator(properties.audience())
        );
        decoder.setJwtValidator(validators);
        return decoder;
    }

    @Bean
    @Qualifier("googleJwtDecoder")
    JwtDecoder googleJwtDecoder(IdentityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(properties.google().jwkSetUri())
                .build();
        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                googleIssuerValidator(),
                audienceValidator(properties.google().clientId())
        );
        decoder.setJwtValidator(validators);
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> googleIssuerValidator() {
        Set<String> allowed = Set.of("accounts.google.com", "https://accounts.google.com");
        return token -> token.getIssuer() != null && allowed.contains(token.getIssuer().toString())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token",
                        "Google token issuer is not allowed",
                        null
                ));
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(String expectedAudience) {
        return token -> expectedAudience != null
                && !expectedAudience.isBlank()
                && token.getAudience().contains(expectedAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token",
                        "Token audience is not allowed",
                        null
                ));
    }
}
