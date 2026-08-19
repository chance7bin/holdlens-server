package com.echoamoy.holdlens.server.domain.auth.model.entity;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserSessionEntityTest {

    @Test
    public void expiresAndRevokesSessions() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 19, 9, 0);
        UserSessionEntity session = UserSessionEntity.create(2L, "hash", now.plusMinutes(1));

        assertTrue(session.isActiveAt(now));
        assertFalse(session.isActiveAt(now.plusMinutes(1)));
        session.revoke(now);
        assertFalse(session.isActiveAt(now));
    }
}
