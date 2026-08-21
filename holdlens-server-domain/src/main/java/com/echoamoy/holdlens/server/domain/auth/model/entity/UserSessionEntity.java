package com.echoamoy.holdlens.server.domain.auth.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor
public class UserSessionEntity {

    private static final Pattern INSTALLATION_ID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

    private Long id;
    private Long userId;
    private String tokenHash;
    private String installationId;
    private String deviceName;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createTime;

    private UserSessionEntity(Long userId, String tokenHash, LocalDateTime expiresAt,
                              String installationId, String deviceName) {
        if (userId == null || userId <= 1) {
            throw new IllegalArgumentException("账号ID不合法");
        }
        if (tokenHash == null || tokenHash.isBlank() || expiresAt == null) {
            throw new IllegalArgumentException("会话参数不合法");
        }
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.installationId = normalizeInstallationId(installationId);
        this.deviceName = normalizeDeviceName(deviceName, this.installationId);
        this.expiresAt = expiresAt;
    }

    public static UserSessionEntity create(Long userId, String tokenHash, LocalDateTime expiresAt,
                                           String installationId, String deviceName) {
        return new UserSessionEntity(userId, tokenHash, expiresAt, installationId, deviceName);
    }

    public boolean isActiveAt(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(LocalDateTime now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public boolean renew(LocalDateTime now, Duration idleTtl, Duration absoluteTtl) {
        if (now == null || idleTtl == null || idleTtl.isNegative() || idleTtl.isZero()
                || absoluteTtl == null || absoluteTtl.isNegative() || absoluteTtl.isZero()
                || createTime == null || !isActiveAt(now)) {
            return false;
        }
        LocalDateTime absoluteExpiresAt = createTime.plus(absoluteTtl);
        if (!absoluteExpiresAt.isAfter(now)) {
            return false;
        }
        LocalDateTime idleExpiresAt = now.plus(idleTtl);
        expiresAt = idleExpiresAt.isBefore(absoluteExpiresAt) ? idleExpiresAt : absoluteExpiresAt;
        return true;
    }

    public void restore(Long id, Long userId, String tokenHash, String installationId, String deviceName,
                        LocalDateTime expiresAt, LocalDateTime revokedAt, LocalDateTime createTime) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.installationId = installationId;
        this.deviceName = deviceName;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.createTime = createTime;
    }

    private static String normalizeInstallationId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!INSTALLATION_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("安装标识不合法");
        }
        return normalized;
    }

    private static String normalizeDeviceName(String value, String installationId) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (installationId == null) {
            throw new IllegalArgumentException("设备名称缺少安装标识");
        }
        String normalized = value.trim();
        if (normalized.length() > 100 || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("设备名称不合法");
        }
        return normalized;
    }
}
