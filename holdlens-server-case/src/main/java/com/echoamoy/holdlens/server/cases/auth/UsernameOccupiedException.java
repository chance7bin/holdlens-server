package com.echoamoy.holdlens.server.cases.auth;

public class UsernameOccupiedException extends RuntimeException {

    public UsernameOccupiedException() {
        super("用户名已占用");
    }
}
