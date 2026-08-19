package com.echoamoy.holdlens.server.cases.auth.impl;

import com.echoamoy.holdlens.server.cases.auth.AuthenticationFailedException;
import com.echoamoy.holdlens.server.cases.auth.AuthenticationResult;
import com.echoamoy.holdlens.server.cases.auth.IAuthenticationCase;
import com.echoamoy.holdlens.server.cases.auth.UsernameOccupiedException;
import com.echoamoy.holdlens.server.domain.auth.adapter.port.IPasswordHasher;
import com.echoamoy.holdlens.server.domain.auth.adapter.port.ISessionTokenPort;
import com.echoamoy.holdlens.server.domain.auth.adapter.repository.IUserAccountRepository;
import com.echoamoy.holdlens.server.domain.auth.adapter.repository.IUserSessionRepository;
import com.echoamoy.holdlens.server.domain.auth.model.entity.UserAccountEntity;
import com.echoamoy.holdlens.server.domain.auth.model.entity.UserSessionEntity;
import com.echoamoy.holdlens.server.domain.auth.model.valobj.IssuedSessionTokenVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthenticationCaseImpl implements IAuthenticationCase {

    private static final String DUMMY_BCRYPT_HASH = "$2a$12$7N2qfNU2TloORuIQbQ2JP.MxGe5dv.UUB9C6uv7IYF9N7Z0IR7cmS";

    @Resource
    private IUserAccountRepository userAccountRepository;

    @Resource
    private IUserSessionRepository userSessionRepository;

    @Resource
    private IPasswordHasher passwordHasher;

    @Resource
    private ISessionTokenPort sessionTokenPort;

    @Value("${holdlens.auth.session-ttl:PT168H}")
    private Duration sessionTtl;

    @Value("${holdlens.auth.login-lock-threshold:5}")
    private int loginLockThreshold;

    @Value("${holdlens.auth.login-lock-duration:PT15M}")
    private Duration loginLockDuration;

    @Override
    @Transactional
    public AuthenticationResult.Account register(String username, String password) {
        UserAccountEntity.validatePassword(password);
        String normalizedUsername = UserAccountEntity.normalizeUsername(username);
        if (userAccountRepository.findByUsername(normalizedUsername) != null) {
            throw new UsernameOccupiedException();
        }
        UserAccountEntity account = UserAccountEntity.register(normalizedUsername, passwordHasher.hash(password));
        if (!userAccountRepository.insert(account)) {
            throw new UsernameOccupiedException();
        }
        return toAccount(account);
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthenticationResult.Login login(String username, String password) {
        String normalizedUsername = UserAccountEntity.normalizeUsername(username);
        UserAccountEntity.validatePassword(password);
        UserAccountEntity account = userAccountRepository.findByUsernameForUpdate(normalizedUsername);
        if (account == null) {
            passwordHasher.matches(password, DUMMY_BCRYPT_HASH);
            throw new AuthenticationFailedException();
        }
        LocalDateTime now = LocalDateTime.now();
        if (!account.canLoginAt(now) || !passwordHasher.matches(password, account.getPasswordHash())) {
            if (account.canLoginAt(now)) {
                account.recordFailedLogin(loginLockThreshold, loginLockDuration, now);
                userAccountRepository.updateLoginState(account);
            }
            throw new AuthenticationFailedException();
        }
        account.resetLoginFailures();
        userAccountRepository.updateLoginState(account);
        IssuedSessionTokenVO issuedToken = sessionTokenPort.issue();
        LocalDateTime expiresAt = now.plus(sessionTtl);
        UserSessionEntity session = UserSessionEntity.create(account.getId(), issuedToken.getTokenHash(), expiresAt);
        userSessionRepository.insert(session);
        return new AuthenticationResult.Login(issuedToken.getRawToken(), expiresAt, toAccount(account));
    }

    @Override
    public AuthenticationResult.AuthenticatedSession authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationFailedException();
        }
        LocalDateTime now = LocalDateTime.now();
        UserSessionEntity session = userSessionRepository.findByTokenHash(sessionTokenPort.hash(rawToken));
        if (session == null || !session.isActiveAt(now)) {
            throw new AuthenticationFailedException();
        }
        UserAccountEntity account = userAccountRepository.findById(session.getUserId());
        if (account == null || !account.isActive()) {
            throw new AuthenticationFailedException();
        }
        return new AuthenticationResult.AuthenticatedSession(session.getUserId(), session.getId());
    }

    @Override
    @Transactional
    public void logout(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new AuthenticationFailedException();
        }
        userSessionRepository.revoke(sessionId);
    }

    @Override
    public AuthenticationResult.Account currentAccount(Long userId) {
        UserAccountEntity account = userAccountRepository.findById(userId);
        if (account == null) {
            throw new AuthenticationFailedException();
        }
        return toAccount(account);
    }

    private AuthenticationResult.Account toAccount(UserAccountEntity account) {
        return new AuthenticationResult.Account(account.getId(), account.getUsername());
    }
}
