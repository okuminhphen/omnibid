package com.omnibid.identity.controller;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwkSetController {

    private final RSAKey rsaKey;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwkSet() {
        return Map.of("keys", java.util.List.of(rsaKey.toPublicJWK().toJSONObject()));
    }
}
