package com.omnibid.identity.controller;

import com.omnibid.identity.dto.SessionResponse;
import com.omnibid.identity.dto.UpdateProfileRequest;
import com.omnibid.identity.dto.UserResponse;
import com.omnibid.identity.service.AuthSessionService;
import com.omnibid.identity.service.AuthenticatedUser;
import com.omnibid.identity.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserAccountService userAccountService;
    private final AuthSessionService sessionService;

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser user = userAccountService.get(userId(jwt));
        return UserResponse.from(user.account(), user.profile());
    }

    @PatchMapping("/profile")
    public UserResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        AuthenticatedUser user = userAccountService.updateProfile(userId(jwt), request);
        return UserResponse.from(user.account(), user.profile());
    }

    @GetMapping("/sessions")
    public List<SessionResponse> sessions(@AuthenticationPrincipal Jwt jwt) {
        UUID currentSessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        return sessionService.listSessions(userId(jwt)).stream()
                .map(session -> SessionResponse.from(session, currentSessionId))
                .toList();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        sessionService.logoutAll(userId(jwt));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId
    ) {
        // A dedicated ownership-safe revoke method is intentionally required;
        // never delete sessions by an unscoped repository call.
        sessionService.revokeSession(userId(jwt), sessionId);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
