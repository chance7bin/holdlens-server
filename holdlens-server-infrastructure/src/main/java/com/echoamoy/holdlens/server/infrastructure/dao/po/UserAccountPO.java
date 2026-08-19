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
public class UserAccountPO {

    private Long id;
    private String username;
    private String passwordHash;
    private String status;
    private Integer failedLoginCount;
    private Date lockedUntil;
    private Date createTime;
    private Date updateTime;

    @Override
    public String toString() {
        return "UserAccountPO{id=" + id + ", username='" + username + "', passwordHash=REDACTED, status='"
                + status + "', failedLoginCount=" + failedLoginCount + ", lockedUntil=" + lockedUntil
                + ", createTime=" + createTime + ", updateTime=" + updateTime + "}";
    }
}
