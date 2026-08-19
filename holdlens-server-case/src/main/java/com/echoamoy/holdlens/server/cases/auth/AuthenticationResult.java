package com.echoamoy.holdlens.server.cases.auth;

import lombok.Getter;

import java.time.LocalDateTime;

public final class AuthenticationResult {

    private AuthenticationResult() {
    }

    @Getter
    public static class Account {
        private final Long id;
        private final String username;

        public Account(Long id, String username) {
            this.id = id;
            this.username = username;
        }
    }

    @Getter
    public static class Login {
        private final String rawToken;
        private final LocalDateTime expiresAt;
        private final Account account;

        public Login(String rawToken, LocalDateTime expiresAt, Account account) {
            this.rawToken = rawToken;
            this.expiresAt = expiresAt;
            this.account = account;
        }

        @Override
        public String toString() {
            return "Login{rawToken=REDACTED, expiresAt=" + expiresAt + ", account=" + account.getId() + "}";
        }
    }

    @Getter
    public static class AuthenticatedSession {
        private final Long userId;
        private final Long sessionId;

        public AuthenticatedSession(Long userId, Long sessionId) {
            this.userId = userId;
            this.sessionId = sessionId;
        }
    }
}
