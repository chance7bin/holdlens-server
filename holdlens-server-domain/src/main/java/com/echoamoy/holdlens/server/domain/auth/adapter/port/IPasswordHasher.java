package com.echoamoy.holdlens.server.domain.auth.adapter.port;

public interface IPasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
