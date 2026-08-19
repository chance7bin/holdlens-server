package com.echoamoy.holdlens.server.domain.auth.model.entity;

import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserAccountEntityTest {

    @Test
    public void normalizesUsernameAndLocksAtConfiguredThreshold() {
        UserAccountEntity account = UserAccountEntity.register("  Alice_1  ", "hash");
        LocalDateTime now = LocalDateTime.of(2026, 8, 19, 9, 0);

        account.recordFailedLogin(2, Duration.ofMinutes(15), now);
        assertTrue(account.canLoginAt(now));
        account.recordFailedLogin(2, Duration.ofMinutes(15), now);

        assertEquals("alice_1", account.getUsername());
        assertEquals(2, account.getFailedLoginCount());
        assertFalse(account.canLoginAt(now));
        assertTrue(account.canLoginAt(now.plusMinutes(15)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPasswordOverUtf8ByteLimit() {
        UserAccountEntity.validatePassword("密".repeat(25));
    }
}
