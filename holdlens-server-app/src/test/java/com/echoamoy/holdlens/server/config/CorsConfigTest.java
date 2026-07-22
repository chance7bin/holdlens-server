package com.echoamoy.holdlens.server.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CorsConfigTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestWebConfig.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void shouldAllowBothLocalClientOriginsOnAnyPort() throws Exception {
        assertAllowedOrigin("http://localhost:5173");
        assertAllowedOrigin("http://localhost:3000");
        assertAllowedOrigin("http://127.0.0.1:5173");
        assertAllowedOrigin("http://127.0.0.1:8080");
    }

    @Test
    public void shouldAllowJsonPostPreflight() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString(HttpHeaders.CONTENT_TYPE)));
    }

    @Test
    public void shouldRejectUnknownOrigin() throws Exception {
        mockMvc.perform(get("/api/test")
                        .header(HttpHeaders.ORIGIN, "https://example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void shouldRejectHttpsLocalOrigin() throws Exception {
        mockMvc.perform(get("/api/test")
                        .header(HttpHeaders.ORIGIN, "https://localhost:5173"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void shouldNotExposeInternalEndpoints() throws Exception {
        mockMvc.perform(get("/internal/test")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private void assertAllowedOrigin(String origin) throws Exception {
        mockMvc.perform(get("/api/test").header(HttpHeaders.ORIGIN, origin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Configuration
    @EnableWebMvc
    @Import({CorsConfig.class, TestController.class})
    static class TestWebConfig {
    }

    @RestController
    static class TestController {

        @GetMapping("/api/test")
        String api() {
            return "ok";
        }

        @PostMapping("/api/test")
        String postApi() {
            return "ok";
        }

        @GetMapping("/internal/test")
        String internal() {
            return "ok";
        }
    }

}
