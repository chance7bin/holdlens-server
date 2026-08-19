package com.echoamoy.holdlens.server.trigger.http.auth;

import com.echoamoy.holdlens.server.cases.auth.AuthenticationFailedException;
import com.echoamoy.holdlens.server.cases.auth.AuthenticationResult;
import com.echoamoy.holdlens.server.cases.auth.IAuthenticationCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class BearerAuthenticationFilter extends OncePerRequestFilter {

    private final AuthProperties authProperties;
    private final IAuthenticationCase authenticationCase;

    public BearerAuthenticationFilter(AuthProperties authProperties, IAuthenticationCase authenticationCase) {
        this.authProperties = authProperties;
        this.authenticationCase = authenticationCase;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            CurrentUser currentUser = authProperties.isFixedMode()
                    ? new CurrentUser(authProperties.getFixedUserId(), null)
                    : authenticateBearerToken(request);
            if (currentUser != null) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private CurrentUser authenticateBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() == "Bearer ".length()) {
            return null;
        }
        try {
            AuthenticationResult.AuthenticatedSession session = authenticationCase.authenticate(
                    authorization.substring("Bearer ".length()));
            if (session == null || session.getUserId() == null || session.getSessionId() == null) {
                return null;
            }
            return new CurrentUser(session.getUserId(), session.getSessionId());
        } catch (AuthenticationFailedException exception) {
            return null;
        }
    }
}
