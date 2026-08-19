package com.echoamoy.holdlens.server.domain.auth.model.entity;

import com.echoamoy.holdlens.server.domain.auth.model.valobj.UserAccountStatusEnumVO;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor
public class UserAccountEntity {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9_]{3,32}");

    private Long id;
    private String username;
    private String passwordHash;
    private UserAccountStatusEnumVO status;
    private int failedLoginCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private UserAccountEntity(String username, String passwordHash) {
        this.username = normalizeUsername(username);
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("密码哈希不合法");
        }
        this.passwordHash = passwordHash;
        this.status = UserAccountStatusEnumVO.ACTIVE;
    }

    public static UserAccountEntity register(String username, String passwordHash) {
        return new UserAccountEntity(username, passwordHash);
    }

    public static String normalizeUsername(String rawUsername) {
        String username = rawUsername == null ? "" : rawUsername.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("用户名不合法");
        }
        return username;
    }

    public static void validatePassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不合法");
        }
        int bytes = rawPassword.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 8 || bytes > 72) {
            throw new IllegalArgumentException("密码不合法");
        }
    }

    public boolean canLoginAt(LocalDateTime now) {
        return status == UserAccountStatusEnumVO.ACTIVE && (lockedUntil == null || !lockedUntil.isAfter(now));
    }

    public boolean isActive() {
        return status == UserAccountStatusEnumVO.ACTIVE;
    }

    public void recordFailedLogin(int threshold, Duration lockDuration, LocalDateTime now) {
        if (threshold < 1 || lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("登录锁定策略不合法");
        }
        failedLoginCount++;
        if (failedLoginCount >= threshold) {
            lockedUntil = now.plus(lockDuration);
        }
    }

    public void resetLoginFailures() {
        failedLoginCount = 0;
        lockedUntil = null;
    }

    public void restore(Long id, String username, String passwordHash, UserAccountStatusEnumVO status,
                        int failedLoginCount, LocalDateTime lockedUntil, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
        this.failedLoginCount = failedLoginCount;
        this.lockedUntil = lockedUntil;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
}
