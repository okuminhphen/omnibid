package com.omnibid.identity.controller;

import com.omnibid.identity.config.IdentityProperties;
import com.omnibid.identity.dto.AuthResponse;
import com.omnibid.identity.dto.GoogleConfigResponse;
import com.omnibid.identity.dto.GoogleLoginRequest;
import com.omnibid.identity.dto.UserResponse;
import com.omnibid.identity.exception.AuthenticationException;
import com.omnibid.identity.service.AuthSessionService;
import com.omnibid.identity.service.AuthenticatedUser;
import com.omnibid.identity.service.GoogleCredentialService;
import com.omnibid.identity.service.GoogleNonceService;
import com.omnibid.identity.service.LoginResult;
import com.omnibid.identity.service.UserAccountService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String GOOGLE_NONCE_COOKIE = "omnibid_google_nonce";

    private final IdentityProperties properties;
    private final GoogleNonceService nonceService;
    private final GoogleCredentialService googleCredentialService;
    private final UserAccountService userAccountService;
    private final AuthSessionService authSessionService;

    @GetMapping("/google/config")
    public ResponseEntity<GoogleConfigResponse> googleConfig() {
        GoogleNonceService.Nonce nonce = nonceService.issue();
        ResponseCookie nonceCookie = ResponseCookie.from(GOOGLE_NONCE_COOKIE, nonce.hash())
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Lax")
                .path("/api/v1/auth/google")
                .maxAge(Duration.ofMinutes(5))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, nonceCookie.toString())
                .body(new GoogleConfigResponse(
                        properties.google().enabled(),
                        properties.google().clientId(),
                        nonce.value()
                ));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            @CookieValue(name = GOOGLE_NONCE_COOKIE, required = false) String nonceHash,
            HttpServletRequest servletRequest
    ) {
        nonceService.validate(request.nonce(), nonceHash);
        GoogleCredentialService.GooglePrincipal googlePrincipal = googleCredentialService.verify(
                request.credential(),
                request.nonce()
        );
        AuthenticatedUser user = userAccountService.findOrCreateGoogle(googlePrincipal);
        LoginResult login = authSessionService.login(
                user,
                servletRequest.getHeader(HttpHeaders.USER_AGENT),
                servletRequest.getRemoteAddr()
        );
        return loginResponse(login, clearNonceCookie());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest servletRequest
    ) {
        String refreshToken = readCookie(servletRequest, properties.refreshCookieName());
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationException("Refresh cookie is missing");
        }
        LoginResult login = authSessionService.refresh(
                refreshToken,
                servletRequest.getHeader(HttpHeaders.USER_AGENT),
                servletRequest.getRemoteAddr()
        );
        return loginResponse(login);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest servletRequest
    ) {
        String refreshToken = readCookie(servletRequest, properties.refreshCookieName());
        authSessionService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    ResponseEntity<AuthResponse> loginResponse(LoginResult login, ResponseCookie... extraCookies) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE, refreshCookie(login.refreshToken()).toString());
        for (ResponseCookie cookie : extraCookies) {
            builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return builder.body(toResponse(login));
    }

    private AuthResponse toResponse(LoginResult login) {
        long expiresIn = Math.max(
                0,
                Duration.between(java.time.Instant.now(), login.accessToken().expiresAt()).toSeconds()
        );
        return new AuthResponse(
                login.accessToken().value(),
                "Bearer",
                expiresIn,
                UserResponse.from(login.user().account(), login.user().profile())
        );
    }

    private ResponseCookie refreshCookie(String rawRefreshToken) {
        return ResponseCookie.from(properties.refreshCookieName(), rawRefreshToken)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(properties.refreshTokenTtl())
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(properties.refreshCookieName(), "")
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie clearNonceCookie() {
        return ResponseCookie.from(GOOGLE_NONCE_COOKIE, "")
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Lax")
                .path("/api/v1/auth/google")
                .maxAge(Duration.ZERO)
                .build();
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
