package com.echoamoy.holdlens.server.domain.auth.model.entity;

import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserSessionEntityTest {

    @Test
    public void expiresAndRevokesSessions() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 19, 9, 0);
        UserSessionEntity session = UserSessionEntity.create(2L, "hash", now.plusMinutes(1), null, null);

        assertTrue(session.isActiveAt(now));
        assertFalse(session.isActiveAt(now.plusMinutes(1)));
        session.revoke(now);
        assertFalse(session.isActiveAt(now));
    }

    @Test
    public void renewsWithinIdleAndAbsoluteExpiryBoundaries() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime now = createdAt.plusDays(85);
        UserSessionEntity session = new UserSessionEntity();
        session.restore(1L, 2L, "hash", null, null, now.plusHours(1), null, createdAt);

        assertTrue(session.renew(now, Duration.ofDays(7), Duration.ofDays(90)));
        assertEquals(createdAt.plusDays(90), session.getExpiresAt());
        assertFalse(session.renew(createdAt.plusDays(90), Duration.ofDays(7), Duration.ofDays(90)));

        session.revoke(now);
        assertFalse(session.renew(now, Duration.ofDays(7), Duration.ofDays(90)));
    }

    @Test
    public void normalizesInstallationIdentityWithoutUsingItAsAuthenticationState() {
        UserSessionEntity session = UserSessionEntity.create(
                2L, "hash", LocalDateTime.now().plusDays(1),
                "A31C5067-2C19-4B45-9F2A-B8FDD4F5B13A", "  iPhone 15 Pro  ");

        assertEquals("a31c5067-2c19-4b45-9f2a-b8fdd4f5b13a", session.getInstallationId());
        assertEquals("iPhone 15 Pro", session.getDeviceName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonV4InstallationIdentity() {
        UserSessionEntity.create(2L, "hash", LocalDateTime.now().plusDays(1),
                "a31c5067-2c19-1b45-9f2a-b8fdd4f5b13a", "iPhone 15 Pro");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDeviceNameWithControlCharacters() {
        UserSessionEntity.create(2L, "hash", LocalDateTime.now().plusDays(1),
                "a31c5067-2c19-4b45-9f2a-b8fdd4f5b13a", "iPhone\n15 Pro");
    }
}
