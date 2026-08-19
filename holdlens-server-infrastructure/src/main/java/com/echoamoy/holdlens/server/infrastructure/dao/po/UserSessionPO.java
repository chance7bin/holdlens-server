package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionPO {

    private Long id;
    private Long userId;
    private String tokenHash;
    private Date expiresAt;
    private Date revokedAt;
    private Date createTime;

    @Override
    public String toString() {
        return "UserSessionPO{id=" + id + ", userId=" + userId + ", tokenHash=REDACTED, expiresAt="
                + expiresAt + ", revokedAt=" + revokedAt + ", createTime=" + createTime + "}";
    }
}
