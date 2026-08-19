package com.echoamoy.holdlens.server.domain.auth.adapter.repository;

import com.echoamoy.holdlens.server.domain.auth.model.entity.UserAccountEntity;

public interface IUserAccountRepository {

    UserAccountEntity findByUsername(String username);

    UserAccountEntity findByUsernameForUpdate(String username);

    UserAccountEntity findById(Long userId);

    boolean insert(UserAccountEntity account);

    void updateLoginState(UserAccountEntity account);
}
