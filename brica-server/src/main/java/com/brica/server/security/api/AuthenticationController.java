package com.brica.server.security.api;

import com.brica.server.security.application.AccessPolicy;
import com.brica.server.security.application.AccountAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationManager authentication;
    private final AccessPolicy access;
    private final SessionAuthenticationStrategy sessions;
    private final HttpSessionSecurityContextRepository contexts;

    public AuthenticationController(AuthenticationManager authentication, AccessPolicy access,
            SessionAuthenticationStrategy sessions, HttpSessionSecurityContextRepository contexts) {
        this.authentication = authentication;
        this.access = access;
        this.sessions = sessions;
        this.contexts = contexts;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token, HttpServletRequest request) {
        String value = token.getToken();
        request.getSession().setMaxInactiveInterval(-1);
        return new CsrfResponse(token.getHeaderName(), value);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public AccountAccess login(@RequestParam("username") String username, @RequestParam("password") String password,
            HttpServletRequest request, HttpServletResponse response) {
        var authenticated = authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        sessions.onAuthentication(authenticated, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        request.getSession().setMaxInactiveInterval(-1);
        return access.current();
    }

    @GetMapping("/session")
    public AccountAccess session() { return access.current(); }

    public record CsrfResponse(String headerName, String token) { }
}
