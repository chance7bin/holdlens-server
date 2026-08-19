package com.echoamoy.holdlens.server.domain.auth.model.valobj;

import lombok.Getter;

@Getter
public class IssuedSessionTokenVO {

    private final String rawToken;
    private final String tokenHash;

    public IssuedSessionTokenVO(String rawToken, String tokenHash) {
        if (rawToken == null || rawToken.isBlank() || tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("会话令牌不合法");
        }
        this.rawToken = rawToken;
        this.tokenHash = tokenHash;
    }

    @Override
    public String toString() {
        return "IssuedSessionTokenVO{token=REDACTED}";
    }
}
