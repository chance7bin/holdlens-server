package com.echoamoy.holdlens.server.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        private String installationId;

        @Size(max = 100)
        private String deviceName;

        @Override
        public String toString() {
            return "Login{username='" + username + "', password=REDACTED}";
        }
    }
}
