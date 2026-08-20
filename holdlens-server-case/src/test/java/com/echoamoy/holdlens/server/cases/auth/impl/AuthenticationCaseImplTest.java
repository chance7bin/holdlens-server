package com.echoamoy.holdlens.server.cases.auth.impl;

import com.echoamoy.holdlens.server.cases.auth.AuthenticationFailedException;
import com.echoamoy.holdlens.server.cases.auth.AuthenticationResult;
import com.echoamoy.holdlens.server.cases.auth.UsernameOccupiedException;
import com.echoamoy.holdlens.server.domain.auth.adapter.port.IPasswordHasher;
import com.echoamoy.holdlens.server.domain.auth.adapter.port.ISessionTokenPort;
import com.echoamoy.holdlens.server.domain.auth.adapter.repository.IUserAccountRepository;
import com.echoamoy.holdlens.server.domain.auth.adapter.repository.IUserSessionRepository;
import com.echoamoy.holdlens.server.domain.auth.model.entity.UserAccountEntity;
import com.echoamoy.holdlens.server.domain.auth.model.entity.UserSessionEntity;
import com.echoamoy.holdlens.server.domain.auth.model.valobj.IssuedSessionTokenVO;
import com.echoamoy.holdlens.server.domain.auth.model.valobj.UserAccountStatusEnumVO;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AuthenticationCaseImplTest {

    @Test
    public void registerNormalizesUsernameStartsAtUserTwoAndRejectsDuplicates() throws Exception {
        Fixture fixture = fixture();

        AuthenticationResult.Account registered = fixture.caseService.register(" Alice_1 ", "correct1");

        assertEquals(Long.valueOf(2L), registered.getId());
        assertEquals("alice_1", registered.getUsername());
        try {
            fixture.caseService.register("ALICE_1", "correct1");
            fail("重复用户名应被拒绝");
        } catch (UsernameOccupiedException ignored) {
        }
    }

    @Test
    public void loginFailurePersistsCounterBeforeThrowing() throws Exception {
        Fixture fixture = fixture();
        fixture.accounts.insert(account(2L, "alice", "correct1"));

        expectAuthenticationFailure(() -> fixture.caseService.login("alice", "incorrect"));

        assertEquals(1, fixture.accounts.findByUsername("alice").getFailedLoginCount());
        assertEquals(1, fixture.accounts.loginStateUpdates);
    }

    @Test
    public void nonexistentAccountUsesDummyHashAndThresholdFailureLocksAccount() throws Exception {
        Fixture fixture = fixture();
        expectAuthenticationFailure(() -> fixture.caseService.login("missing", "incorrect"));
        assertEquals(1, fixture.passwordHasher.dummyHashMatches);

        UserAccountEntity account = account(2L, "alice", "correct1");
        fixture.accounts.insert(account);
        for (int index = 0; index < 5; index++) {
            expectAuthenticationFailure(() -> fixture.caseService.login("alice", "incorrect"));
        }
        assertEquals(5, account.getFailedLoginCount());
        assertNotNull(account.getLockedUntil());
        assertEquals(5, fixture.accounts.loginStateUpdates);
    }

    @Test
    public void successfulLoginResetsFailuresAndCreatesOpaqueSession() throws Exception {
        Fixture fixture = fixture();
        UserAccountEntity account = account(2L, "alice", "correct1");
        account.recordFailedLogin(5, Duration.ofMinutes(15), LocalDateTime.now());
        fixture.accounts.insert(account);

        AuthenticationResult.Login login = fixture.caseService.login(" ALICE ", "correct1");

        assertEquals("raw-token", login.getRawToken());
        assertEquals(0, account.getFailedLoginCount());
        assertEquals(1, fixture.sessions.sessions.size());
        assertEquals("token-hash", fixture.sessions.sessions.get(1L).getTokenHash());
    }

    @Test
    public void successfulLoginRevokesPreviousActiveSession() throws Exception {
        Fixture fixture = fixture();
        fixture.accounts.insert(account(2L, "alice", "correct1"));
        UserSessionEntity oldSession = UserSessionEntity.create(
                2L, "old-hash", LocalDateTime.now().plusDays(1));
        fixture.sessions.insert(oldSession);

        fixture.caseService.login("alice", "correct1");

        assertFalse(oldSession.isActiveAt(LocalDateTime.now()));
        assertEquals(2, fixture.sessions.sessions.size());
        expectAuthenticationFailure(() -> fixture.caseService.renew(oldSession.getId()));
    }

    @Test
    public void authenticatesOnlyActiveSessionAndLogoutRevokesIt() throws Exception {
        Fixture fixture = fixture();
        fixture.accounts.insert(account(2L, "alice", "correct1"));
        UserSessionEntity session = UserSessionEntity.create(2L, "token-hash", LocalDateTime.now().plusHours(1));
        fixture.sessions.insert(session);

        AuthenticationResult.AuthenticatedSession authenticated = fixture.caseService.authenticate("raw-token");
        assertEquals(Long.valueOf(2L), authenticated.getUserId());
        assertNotNull(authenticated.getSessionId());
        fixture.caseService.logout(authenticated.getSessionId());
        expectAuthenticationFailure(() -> fixture.caseService.authenticate("raw-token"));
    }

    @Test
    public void rejectsExpiredSessionAndReturnsOnlyCurrentAccountSummary() throws Exception {
        Fixture fixture = fixture();
        UserAccountEntity account = account(2L, "alice", "correct1");
        fixture.accounts.insert(account);
        UserSessionEntity expired = UserSessionEntity.create(2L, "token-hash", LocalDateTime.now().minusSeconds(1));
        fixture.sessions.insert(expired);

        expectAuthenticationFailure(() -> fixture.caseService.authenticate("raw-token"));
        AuthenticationResult.Account current = fixture.caseService.currentAccount(2L);
        assertEquals(Long.valueOf(2L), current.getId());
        assertEquals("alice", current.getUsername());
    }

    @Test
    public void rejectsSessionWhoseAccountDoesNotExist() throws Exception {
        Fixture fixture = fixture();
        UserSessionEntity session = UserSessionEntity.create(2L, "token-hash", LocalDateTime.now().plusHours(1));
        fixture.sessions.insert(session);

        expectAuthenticationFailure(() -> fixture.caseService.authenticate("raw-token"));
    }

    @Test
    public void renewsSessionWithoutPassingAbsoluteExpiry() throws Exception {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = now.minusDays(85);
        UserSessionEntity session = new UserSessionEntity();
        session.restore(9L, 2L, "token-hash", now.plusHours(1), null, createdAt);
        fixture.sessions.sessions.put(9L, session);

        AuthenticationResult.Renewal renewal = fixture.caseService.renew(9L);

        assertEquals(createdAt.plusDays(90), renewal.getExpiresAt());
        assertEquals(1, fixture.sessions.expiryUpdates);
    }

    @Test
    public void rejectsRenewalAtAbsoluteExpiry() throws Exception {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.now();
        UserSessionEntity session = new UserSessionEntity();
        session.restore(9L, 2L, "token-hash", now.plusHours(1), null, now.minusDays(90));
        fixture.sessions.sessions.put(9L, session);

        expectAuthenticationFailure(() -> fixture.caseService.renew(9L));
        assertEquals(0, fixture.sessions.expiryUpdates);
    }

    private Fixture fixture() throws Exception {
        AuthenticationCaseImpl caseService = new AuthenticationCaseImpl();
        FakeAccounts accounts = new FakeAccounts();
        FakeSessions sessions = new FakeSessions();
        set(caseService, "userAccountRepository", accounts);
        set(caseService, "userSessionRepository", sessions);
        FakePasswordHasher passwordHasher = new FakePasswordHasher();
        set(caseService, "passwordHasher", passwordHasher);
        set(caseService, "sessionTokenPort", new FakeTokenPort());
        set(caseService, "sessionTtl", Duration.ofDays(7));
        set(caseService, "sessionAbsoluteTtl", Duration.ofDays(90));
        set(caseService, "loginLockThreshold", 5);
        set(caseService, "loginLockDuration", Duration.ofMinutes(15));
        return new Fixture(caseService, accounts, sessions, passwordHasher);
    }

    private UserAccountEntity account(Long id, String username, String passwordHash) {
        UserAccountEntity account = UserAccountEntity.register(username, passwordHash);
        account.restore(id, username, passwordHash, UserAccountStatusEnumVO.ACTIVE, 0, null, null, null);
        return account;
    }

    private void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void expectAuthenticationFailure(Runnable action) {
        try {
            action.run();
            fail("应抛出统一认证失败异常");
        } catch (AuthenticationFailedException ignored) {
        }
    }

    private record Fixture(AuthenticationCaseImpl caseService, FakeAccounts accounts, FakeSessions sessions,
                           FakePasswordHasher passwordHasher) {
    }

    private static class FakeAccounts implements IUserAccountRepository {
        private final Map<String, UserAccountEntity> accounts = new HashMap<>();
        private int loginStateUpdates;

        @Override
        public UserAccountEntity findByUsername(String username) {
            return accounts.get(username);
        }

        @Override
        public UserAccountEntity findByUsernameForUpdate(String username) {
            return accounts.get(username);
        }

        @Override
        public UserAccountEntity findById(Long userId) {
            return accounts.values().stream().filter(account -> account.getId().equals(userId)).findFirst().orElse(null);
        }

        @Override
        public boolean insert(UserAccountEntity account) {
            if (accounts.containsKey(account.getUsername())) {
                return false;
            }
            if (account.getId() == null) {
                account.restore((long) accounts.size() + 2, account.getUsername(), account.getPasswordHash(),
                        account.getStatus(), account.getFailedLoginCount(), account.getLockedUntil(), null, null);
            }
            accounts.put(account.getUsername(), account);
            return true;
        }

        @Override
        public void updateLoginState(UserAccountEntity account) {
            loginStateUpdates++;
        }
    }

    private static class FakeSessions implements IUserSessionRepository {
        private final Map<Long, UserSessionEntity> sessions = new HashMap<>();
        private int expiryUpdates;

        @Override
        public void insert(UserSessionEntity session) {
            long id = sessions.size() + 1L;
            LocalDateTime createTime = session.getCreateTime() == null ? LocalDateTime.now() : session.getCreateTime();
            session.restore(id, session.getUserId(), session.getTokenHash(), session.getExpiresAt(),
                    session.getRevokedAt(), createTime);
            sessions.put(id, session);
        }

        @Override
        public UserSessionEntity findByTokenHash(String tokenHash) {
            return sessions.values().stream().filter(session -> session.getTokenHash().equals(tokenHash)).findFirst().orElse(null);
        }

        @Override
        public UserSessionEntity findByIdForUpdate(Long sessionId) {
            return sessions.get(sessionId);
        }

        @Override
        public void revokeActiveByUserId(Long userId) {
            LocalDateTime now = LocalDateTime.now();
            sessions.values().stream()
                    .filter(session -> session.getUserId().equals(userId) && session.isActiveAt(now))
                    .forEach(session -> session.revoke(now));
        }

        @Override
        public void revoke(Long sessionId) {
            UserSessionEntity session = sessions.get(sessionId);
            if (session != null) {
                session.revoke(LocalDateTime.now());
            }
        }

        @Override
        public boolean updateExpiresAt(UserSessionEntity session) {
            expiryUpdates++;
            return sessions.containsKey(session.getId());
        }
    }

    private static class FakePasswordHasher implements IPasswordHasher {
        private int dummyHashMatches;

        @Override
        public String hash(String rawPassword) {
            return rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            if (passwordHash.startsWith("$2a$12$")) {
                dummyHashMatches++;
            }
            return rawPassword.equals(passwordHash);
        }
    }

    private static class FakeTokenPort implements ISessionTokenPort {
        @Override
        public IssuedSessionTokenVO issue() {
            return new IssuedSessionTokenVO("raw-token", "token-hash");
        }

        @Override
        public String hash(String rawToken) {
            return "raw-token".equals(rawToken) ? "token-hash" : "other-hash";
        }
    }
}
