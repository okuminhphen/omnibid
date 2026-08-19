package com.omnibid.identity.security;

import com.omnibid.identity.config.IdentityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BrowserOriginFilter extends OncePerRequestFilter {

    private static final Set<String> COOKIE_MUTATING_PATHS = Set.of(
            "/api/v1/auth/google",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/dev/login"
    );

    private final IdentityProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !COOKIE_MUTATING_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);

        // Non-browser clients such as CLI/Postman normally omit Origin. Browsers
        // include it for these fetch requests, so an untrusted same-site origin
        // cannot silently rotate or revoke an HttpOnly-cookie session.
        if (origin != null && !properties.allowedOrigins().contains(origin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
