package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.auth.model.valobj.IssuedSessionTokenVO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.UserAccountPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.UserSessionPO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class AuthSecurityPortTest {

    @Test
    public void bcryptHashIsSaltedAndVerifiable() {
        BCryptPasswordHasher hasher = new BCryptPasswordHasher();

        String firstHash = hasher.hash("valid-password");
        String secondHash = hasher.hash("valid-password");

        assertNotEquals(firstHash, secondHash);
        assertTrue(hasher.matches("valid-password", firstHash));
        assertFalse(hasher.matches("wrong-password", firstHash));
    }

    @Test
    public void issuedTokenUsesStableSha256Hash() {
        SecureRandomSessionTokenPort port = new SecureRandomSessionTokenPort();

        IssuedSessionTokenVO token = port.issue();

        assertEquals(43, token.getRawToken().length());
        assertEquals(64, token.getTokenHash().length());
        assertEquals(token.getTokenHash(), port.hash(token.getRawToken()));
        assertNotEquals(token.getRawToken(), token.getTokenHash());
    }

    @Test
    public void persistenceObjectsRedactCredentialDerivatives() {
        assertFalse(UserAccountPO.builder().passwordHash("password-hash").build()
                .toString().contains("password-hash"));
        assertFalse(UserSessionPO.builder().tokenHash("token-hash").build()
                .toString().contains("token-hash"));
    }
}
