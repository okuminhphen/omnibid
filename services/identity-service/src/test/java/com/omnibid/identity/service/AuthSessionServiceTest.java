package com.omnibid.identity.service;

import com.omnibid.identity.config.IdentityProperties;
import com.omnibid.identity.domain.AuthSession;
import com.omnibid.identity.domain.AuthSessionStatus;
import com.omnibid.identity.domain.UserAccount;
import com.omnibid.identity.domain.UserProfile;
import com.omnibid.identity.exception.AuthenticationException;
import com.omnibid.identity.repository.AuthSessionRepository;
import com.omnibid.identity.security.TokenHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock
    private AuthSessionRepository sessionRepository;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private AccessTokenService accessTokenService;

    private final TokenHashing tokenHashing = new TokenHashing();
    private AuthSessionService service;
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        IdentityProperties properties = new IdentityProperties(
                "http://identity.test",
                "omnibid-api",
                Duration.ofMinutes(10),
                Duration.ofDays(30),
                "omnibid_refresh",
                false,
                "Lax",
                List.of("http://localhost:3000"),
                new IdentityProperties.Google(false, "", "https://example.test/jwks"),
                new IdentityProperties.Kafka("identity-events")
        );
        service = new AuthSessionService(
                sessionRepository,
                userAccountService,
                accessTokenService,
                tokenHashing,
                properties
        );

        UserAccount account = new UserAccount();
        account.setId(UUID.randomUUID());
        UserProfile profile = new UserProfile();
        profile.setUserId(account.getId());
        authenticatedUser = new AuthenticatedUser(account, profile);
    }

    @Test
    void loginPersistsOnlyRefreshTokenHash() {
        stubAccessToken();
        LoginResult result = service.login(authenticatedUser, "browser", "127.0.0.1");

        ArgumentCaptor<AuthSession> sessionCaptor = ArgumentCaptor.forClass(AuthSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        AuthSession persisted = sessionCaptor.getValue();

        assertThat(result.refreshToken()).hasSize(43);
        assertThat(persisted.getRefreshTokenHash())
                .isEqualTo(tokenHashing.sha256(result.refreshToken()))
                .isNotEqualTo(result.refreshToken());
        assertThat(persisted.getStatus()).isEqualTo(AuthSessionStatus.ACTIVE);
        assertThat(persisted.getTokenFamilyId()).isNotNull();
    }

    @Test
    void refreshRotatesTokenWithinSameFamily() {
        stubAccessToken();
        String rawToken = tokenHashing.newOpaqueToken();
        AuthSession current = activeSession(rawToken);
        when(sessionRepository.findForUpdateByRefreshTokenHash(tokenHashing.sha256(rawToken)))
                .thenReturn(Optional.of(current));
        when(userAccountService.get(current.getUserId())).thenReturn(authenticatedUser);

        LoginResult result = service.refresh(rawToken, "new browser metadata", "10.0.0.1");

        ArgumentCaptor<AuthSession> replacementCaptor = ArgumentCaptor.forClass(AuthSession.class);
        verify(sessionRepository).save(replacementCaptor.capture());
        AuthSession replacement = replacementCaptor.getValue();
        assertThat(current.getStatus()).isEqualTo(AuthSessionStatus.ROTATED);
        assertThat(current.getReplacedBySessionId()).isEqualTo(replacement.getId());
        assertThat(replacement.getTokenFamilyId()).isEqualTo(current.getTokenFamilyId());
        assertThat(result.refreshToken()).isNotEqualTo(rawToken);
    }

    @Test
    void reuseOfRotatedTokenRevokesEntireFamily() {
        String rawToken = tokenHashing.newOpaqueToken();
        AuthSession current = activeSession(rawToken);
        current.setStatus(AuthSessionStatus.ROTATED);
        when(sessionRepository.findForUpdateByRefreshTokenHash(tokenHashing.sha256(rawToken)))
                .thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.refresh(rawToken, "browser", "127.0.0.1"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("reuse");

        verify(sessionRepository).revokeFamily(
                eq(current.getTokenFamilyId()),
                eq(AuthSessionStatus.REVOKED),
                any(Instant.class)
        );
        verify(userAccountService, never()).get(any());
    }

    private AuthSession activeSession(String rawToken) {
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID());
        session.setUserId(authenticatedUser.account().getId());
        session.setTokenFamilyId(UUID.randomUUID());
        session.setRefreshTokenHash(tokenHashing.sha256(rawToken));
        session.setStatus(AuthSessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        return session;
    }

    private void stubAccessToken() {
        when(accessTokenService.issue(any(), any()))
                .thenReturn(new IssuedAccessToken("signed-access-token", Instant.now().plusSeconds(600)));
    }
}
