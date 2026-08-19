package com.echoamoy.holdlens.server.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public final class AuthenticationDTO {

    private AuthenticationDTO() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Account {
        private Long id;
        private String username;

        public Account(Long id, String username) {
            this.id = id;
            this.username = username;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Login {
        private String accessToken;
        private LocalDateTime expiresAt;
        private Account account;

        public Login(String accessToken, LocalDateTime expiresAt, Account account) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
            this.account = account;
        }

        @Override
        public String toString() {
            return "Login{accessToken=REDACTED, expiresAt=" + expiresAt + ", account="
                    + (account == null ? null : account.getId()) + "}";
        }
    }
}
