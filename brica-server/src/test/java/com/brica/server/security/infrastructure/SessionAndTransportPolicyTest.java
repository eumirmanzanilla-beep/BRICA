package com.brica.server.security.infrastructure;

import com.brica.server.security.api.SecurityProblemWriter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionAndTransportPolicyTest {
    @Test
    void shouldDisableTemporalExpirationAndKeepOperationalCookiesSecure() throws Exception {
        var context = new MockServletContext();
        new SecurityConfiguration().sessionPolicy(false).onStartup(context);
        assertEquals(-1, context.getSessionTimeout());
        assertTrue(context.getSessionCookieConfig().isHttpOnly());
        assertTrue(context.getSessionCookieConfig().isSecure());
    }

    @Test
    void shouldRejectHttpUnlessExplicitlyEnabledAndNotTrustForwardedHeader() throws Exception {
        var problems = mock(SecurityProblemWriter.class);
        var chain = mock(jakarta.servlet.FilterChain.class);
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("X-Forwarded-Proto", "https");
        var response = new MockHttpServletResponse();
        new SecureTransportFilter(false, problems).doFilter(request, response, chain);
        verify(problems).httpsRequired(response);
        verifyNoInteractions(chain);
        request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        new SecureTransportFilter(true, problems).doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
