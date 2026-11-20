package com.brica.server.security.infrastructure;

import java.io.IOException;

import com.brica.server.security.api.SecurityProblemWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

final class SecureTransportFilter extends OncePerRequestFilter {
    private final boolean allowInsecure;
    private final SecurityProblemWriter problems;
    SecureTransportFilter(boolean allowInsecure, SecurityProblemWriter problems) {
        this.allowInsecure = allowInsecure;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (!allowInsecure && !request.isSecure()
                && !request.getRequestURI().equals(request.getContextPath() + "/api/v1/health")) {
            problems.httpsRequired(response);
            return;
        }
        chain.doFilter(request, response);
    }
}
