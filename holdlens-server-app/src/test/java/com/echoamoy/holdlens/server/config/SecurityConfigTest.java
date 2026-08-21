package com.echoamoy.holdlens.server.config;

import com.echoamoy.holdlens.server.cases.auth.AuthenticationFailedException;
import com.echoamoy.holdlens.server.cases.auth.AuthenticationResult;
import com.echoamoy.holdlens.server.cases.auth.IAuthenticationCase;
import com.echoamoy.holdlens.server.trigger.http.auth.AuthProperties;
import com.echoamoy.holdlens.server.trigger.http.auth.BearerAuthenticationFilter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityConfigTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestWebConfig.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class))
                .build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void sessionModeProtectsApiButExcludesInternalAndAnonymousEndpoints() throws Exception {
        mockMvc.perform(get("/api/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"code\":\"0401\",\"info\":\"未认证\"}"));
        mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/session/renew")).andExpect(status().isUnauthorized());
        mockMvc.perform(options("/api/test")).andExpect(status().isOk());
        mockMvc.perform(get("/internal/test")).andExpect(status().isOk());
    }

    @Test
    public void sessionModeUsesValidatedBearerToken() throws Exception {
        mockMvc.perform(get("/api/test").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
        mockMvc.perform(post("/api/auth/session/renew").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/test").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void fixedModeIsRejectedOutsideDevAndTestProfiles() {
        AuthProperties properties = new AuthProperties(new MockEnvironment());
        properties.setMode("fixed");

        Assert.assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    public void fixedModeEstablishesDevUserWithoutBearerTokenAndClearsContext() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        AuthProperties properties = new AuthProperties(environment);
        properties.setMode("fixed");
        properties.setFixedUserId(1L);
        properties.validate();
        BearerAuthenticationFilter filter = new BearerAuthenticationFilter(
                properties, context.getBean(IAuthenticationCase.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        filter.doFilter(request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) -> {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Assert.assertEquals(Long.valueOf(1L),
                    ((com.echoamoy.holdlens.server.trigger.http.auth.CurrentUser) principal).userId());
        });

        Assert.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, TestController.class})
    static class TestWebConfig {
        @Bean
        AuthProperties authProperties() {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles("dev");
            AuthProperties properties = new AuthProperties(environment);
            properties.setMode("session");
            return properties;
        }

        @Bean
        IAuthenticationCase authenticationCase() {
            return new IAuthenticationCase() {
                @Override public AuthenticationResult.Account register(String username, String password) { return null; }
                @Override public AuthenticationResult.Login login(
                        String username, String password, String installationId, String deviceName) { return null; }
                @Override public AuthenticationResult.AuthenticatedSession authenticate(String rawToken) {
                    if (!"valid".equals(rawToken)) throw new AuthenticationFailedException();
                    return new AuthenticationResult.AuthenticatedSession(2L, 3L);
                }
                @Override public AuthenticationResult.Renewal renew(Long sessionId) { return null; }
                @Override public void logout(Long sessionId) { }
                @Override public AuthenticationResult.Account currentAccount(Long userId) { return null; }
            };
        }

        @Bean
        BearerAuthenticationFilter bearerAuthenticationFilter(AuthProperties properties, IAuthenticationCase authenticationCase) {
            return new BearerAuthenticationFilter(properties, authenticationCase);
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/api/test") String api() { return "ok"; }
        @PostMapping("/api/auth/login") String login() { return "ok"; }
        @PostMapping("/api/auth/session/renew") String renew() { return "ok"; }
        @GetMapping("/internal/test") String internal() { return "ok"; }
    }
}
