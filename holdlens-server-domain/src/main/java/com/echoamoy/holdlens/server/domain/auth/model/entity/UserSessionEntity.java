package com.echoamoy.holdlens.server.domain.auth.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class UserSessionEntity {

    private Long id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createTime;

    private UserSessionEntity(Long userId, String tokenHash, LocalDateTime expiresAt) {
        if (userId == null || userId <= 1) {
            throw new IllegalArgumentException("账号ID不合法");
        }
        if (tokenHash == null || tokenHash.isBlank() || expiresAt == null) {
            throw new IllegalArgumentException("会话参数不合法");
        }
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static UserSessionEntity create(Long userId, String tokenHash, LocalDateTime expiresAt) {
        return new UserSessionEntity(userId, tokenHash, expiresAt);
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

    public void restore(Long id, Long userId, String tokenHash, LocalDateTime expiresAt,
                        LocalDateTime revokedAt, LocalDateTime createTime) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.createTime = createTime;
    }
}
