package com.echoamoy.holdlens.server.domain.auth.adapter.repository;

import com.echoamoy.holdlens.server.domain.auth.model.entity.UserSessionEntity;

public interface IUserSessionRepository {

    void insert(UserSessionEntity session);

    UserSessionEntity findByTokenHash(String tokenHash);

    UserSessionEntity findByIdForUpdate(Long sessionId);

    void revokeActiveByUserId(Long userId);

    void revoke(Long sessionId);

    boolean updateExpiresAt(UserSessionEntity session);
}
