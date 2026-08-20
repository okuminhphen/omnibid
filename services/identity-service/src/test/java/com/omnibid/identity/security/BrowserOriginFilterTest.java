package com.omnibid.identity.security;

import com.omnibid.identity.config.IdentityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserOriginFilterTest {

    private final BrowserOriginFilter filter = new BrowserOriginFilter(new IdentityProperties(
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
    ));

    @Test
    void allowsConfiguredBrowserOrigin() throws Exception {
        MockHttpServletRequest request = request("http://localhost:3000");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void rejectsUntrustedOriginBeforeCookieMutation() throws Exception {
        MockHttpServletRequest request = request("https://attacker.example");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    private MockHttpServletRequest request(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.addHeader("Origin", origin);
        return request;
    }
}
