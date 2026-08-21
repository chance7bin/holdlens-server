package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.request.AuthenticationRequestDTO;
import com.echoamoy.holdlens.server.api.dto.AuthenticationDTO;
import org.junit.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

public class AuthControllerTest {

    @Test
    public void exposesOnlySpecifiedAuthenticationEndpointsWithGetAndPost() throws Exception {
        assertArrayEquals(new String[]{"/api/auth"}, AuthController.class.getAnnotation(RequestMapping.class).value());
        assertPost(AuthController.class.getMethod("register", AuthenticationRequestDTO.Register.class), "/register");
        assertPost(AuthController.class.getMethod("login", AuthenticationRequestDTO.Login.class), "/login");
        assertPost(AuthController.class.getMethod("renewSession"), "/session/renew");
        assertPost(AuthController.class.getMethod("logout"), "/logout");
        assertGet(AuthController.class.getMethod("currentAccount"), "/me");
    }

    @Test
    public void credentialRequestsDoNotRevealPasswordsInToString() {
        AuthenticationRequestDTO.Login request = new AuthenticationRequestDTO.Login();
        request.setUsername("alice");
        request.setPassword("super-secret-password");
        request.setInstallationId("a31c5067-2c19-4b45-9f2a-b8fdd4f5b13a");
        request.setDeviceName("iPhone 15 Pro");

        assertFalse(request.toString().contains("super-secret-password"));
        assertFalse(request.toString().contains("a31c5067-2c19-4b45-9f2a-b8fdd4f5b13a"));
        assertFalse(request.toString().contains("iPhone 15 Pro"));

        AuthenticationDTO.Login response = new AuthenticationDTO.Login(
                "raw-session-token", null, new AuthenticationDTO.Account(2L, "alice")
        );
        assertFalse(response.toString().contains("raw-session-token"));
    }

    private void assertPost(Method method, String path) {
        assertArrayEquals(new String[]{path}, method.getAnnotation(PostMapping.class).value());
    }

    private void assertGet(Method method, String path) {
        assertArrayEquals(new String[]{path}, method.getAnnotation(GetMapping.class).value());
    }
}
