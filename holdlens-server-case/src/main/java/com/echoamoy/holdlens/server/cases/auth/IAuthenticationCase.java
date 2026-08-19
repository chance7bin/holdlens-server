package com.echoamoy.holdlens.server.cases.auth;

public interface IAuthenticationCase {

    AuthenticationResult.Account register(String username, String password);

    AuthenticationResult.Login login(String username, String password);

    AuthenticationResult.AuthenticatedSession authenticate(String rawToken);

    void logout(Long sessionId);

    AuthenticationResult.Account currentAccount(Long userId);
}
