package com.echoamoy.holdlens.server.cases.auth;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("认证失败");
    }
}
