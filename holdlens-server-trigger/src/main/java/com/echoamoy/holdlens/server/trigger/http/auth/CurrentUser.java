package com.echoamoy.holdlens.server.trigger.http.auth;

/**
 * 由认证过滤器在单个 HTTP 请求内建立的可信身份。
 */
public record CurrentUser(Long userId, Long sessionId) {
}
