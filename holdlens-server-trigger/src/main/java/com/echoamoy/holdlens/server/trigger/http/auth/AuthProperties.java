package com.echoamoy.holdlens.server.trigger.http.auth;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "holdlens.auth")
public class AuthProperties {

    private String mode = "fixed";
    private Long fixedUserId = 1L;

    private final Environment environment;

    public AuthProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        if (!"fixed".equals(mode) && !"session".equals(mode)) {
            throw new IllegalStateException("holdlens.auth.mode 只能为 fixed 或 session");
        }
        if ("fixed".equals(mode) && (fixedUserId == null || fixedUserId <= 0)) {
            throw new IllegalStateException("fixed 模式需要正数 fixed-user-id");
        }
        if ("fixed".equals(mode) && Arrays.stream(environment.getActiveProfiles())
                .noneMatch(profile -> "dev".equals(profile) || "test".equals(profile))) {
            throw new IllegalStateException("非 dev/test 环境禁止使用 fixed 认证模式");
        }
    }

    public boolean isFixedMode() {
        return "fixed".equals(mode);
    }
}
