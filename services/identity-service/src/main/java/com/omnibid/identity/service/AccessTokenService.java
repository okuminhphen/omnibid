package com.omnibid.identity.service;

import com.nimbusds.jose.jwk.RSAKey;
import com.omnibid.identity.config.IdentityProperties;
import com.omnibid.identity.domain.Role;
import com.omnibid.identity.domain.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessTokenService {

    private final JwtEncoder jwtEncoder;
    private final RSAKey rsaKey;
    private final IdentityProperties properties;

    public IssuedAccessToken issue(UserAccount user, UUID sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        List<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .map(Enum::name)
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .audience(List.of(properties.audience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("sid", sessionId.toString())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(rsaKey.getKeyID())
                .build();

        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        return new IssuedAccessToken(value, expiresAt);
    }
}
