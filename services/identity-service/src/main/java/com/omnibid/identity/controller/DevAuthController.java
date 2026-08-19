package com.omnibid.identity.controller;

import com.omnibid.identity.dto.AuthResponse;
import com.omnibid.identity.dto.DevLoginRequest;
import com.omnibid.identity.service.AuthSessionService;
import com.omnibid.identity.service.AuthenticatedUser;
import com.omnibid.identity.service.LoginResult;
import com.omnibid.identity.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/dev")
@Profile("local")
@RequiredArgsConstructor
public class DevAuthController {

    private final UserAccountService userAccountService;
    private final AuthSessionService authSessionService;
    private final AuthController authController;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody DevLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthenticatedUser user = userAccountService.findOrCreateDevUser(request.alias());
        LoginResult login = authSessionService.login(
                user,
                servletRequest.getHeader(HttpHeaders.USER_AGENT),
                servletRequest.getRemoteAddr()
        );
        return authController.loginResponse(login);
    }
}
