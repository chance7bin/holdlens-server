package com.echoamoy.holdlens.server.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public final class AuthenticationRequestDTO {

    private AuthenticationRequestDTO() {
    }

    @Getter
    @Setter
    public static class Register {
        @NotBlank
        private String username;

        @NotBlank
        private String password;

        @Override
        public String toString() {
            return "Register{username='" + username + "', password=REDACTED}";
        }
    }

    @Getter
    @Setter
    public static class Login {
        @NotBlank
        private String username;

        @NotBlank
        private String password;

        @Override
        public String toString() {
            return "Login{username='" + username + "', password=REDACTED}";
        }
    }
}
