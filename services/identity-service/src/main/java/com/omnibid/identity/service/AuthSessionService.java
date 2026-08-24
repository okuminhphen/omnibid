package com.omnibid.identity.service;

import com.omnibid.identity.config.IdentityProperties;
import com.omnibid.identity.domain.AuthSession;
import com.omnibid.identity.domain.AuthSessionStatus;
import com.omnibid.identity.domain.UserAccount;
import com.omnibid.identity.exception.AuthenticationException;
import com.omnibid.identity.repository.AuthSessionRepository;
import com.omnibid.identity.security.TokenHashing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionRepository sessionRepository;
    private final UserAccountService userAccountService;
    private final AccessTokenService accessTokenService;
    private final TokenHashing tokenHashing;
    private final IdentityProperties properties;

    @Transactional
    public LoginResult login(AuthenticatedUser user, String userAgent, String remoteAddress) {
        SessionToken sessionToken = createSession(
                user.account(),
                UUID.randomUUID(),
                userAgent,
                remoteAddress
        );
        return new LoginResult(sessionToken.accessToken(), sessionToken.rawRefreshToken(), user);
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public LoginResult refresh(String rawRefreshToken, String userAgent, String remoteAddress) {
        String tokenHash = tokenHashing.sha256(rawRefreshToken);
        AuthSession current = sessionRepository.findForUpdateByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationException("Refresh session is invalid"));
        Instant now = Instant.now();

        if (current.getStatus() == AuthSessionStatus.ROTATED) {
            sessionRepository.revokeFamily(
                    current.getTokenFamilyId(),
                    AuthSessionStatus.REVOKED,
                    now
            );
            throw new AuthenticationException("Refresh token reuse was detected");
        }
        if (!current.isUsableAt(now)) {
            if (current.getExpiresAt().isBefore(now)) {
                current.setStatus(AuthSessionStatus.EXPIRED);
            }
            throw new AuthenticationException("Refresh session has expired or was revoked");
        }

        AuthenticatedUser user = userAccountService.get(current.getUserId());
        SessionToken replacement = createSession(
                user.account(),
                current.getTokenFamilyId(),
                userAgent,
                remoteAddress
        );
        current.setStatus(AuthSessionStatus.ROTATED);
        current.setLastUsedAt(now);
        current.setReplacedBySessionId(replacement.sessionId());
        return new LoginResult(replacement.accessToken(), replacement.rawRefreshToken(), user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        sessionRepository.findForUpdateByRefreshTokenHash(tokenHashing.sha256(rawRefreshToken))
                .ifPresent(session -> {
                    if (session.getStatus() == AuthSessionStatus.ACTIVE) {
                        session.setStatus(AuthSessionStatus.REVOKED);
                        session.setRevokedAt(Instant.now());
                    }
                });
    }

    @Transactional
    public void logoutAll(UUID userId) {
        sessionRepository.revokeAllForUser(
                userId,
                AuthSessionStatus.REVOKED,
                Instant.now()
        );
    }

    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        AuthSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AuthenticationException("Session was not found"));
        if (session.getStatus() == AuthSessionStatus.ACTIVE) {
            session.setStatus(AuthSessionStatus.REVOKED);
            session.setRevokedAt(Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public List<AuthSession> listSessions(UUID userId) {
        return sessionRepository.findTop20ByUserIdAndStatusOrderByCreatedAtDesc(
                userId,
                AuthSessionStatus.ACTIVE
        );
    }

    private SessionToken createSession(
            UserAccount user,
            UUID familyId,
            String userAgent,
            String remoteAddress
    ) {
        String rawRefreshToken = tokenHashing.newOpaqueToken();
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID());
        session.setUserId(user.getId());
        session.setTokenFamilyId(familyId);
        session.setRefreshTokenHash(tokenHashing.sha256(rawRefreshToken));
        session.setUserAgent(truncate(userAgent, 500));
        session.setIpHash(remoteAddress == null ? null : tokenHashing.sha256(remoteAddress));
        session.setStatus(AuthSessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().plus(properties.refreshTokenTtl()));
        sessionRepository.save(session);

        IssuedAccessToken accessToken = accessTokenService.issue(user, session.getId());
        return new SessionToken(session.getId(), accessToken, rawRefreshToken);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record SessionToken(
            UUID sessionId,
            IssuedAccessToken accessToken,
            String rawRefreshToken
    ) {
    }
}
