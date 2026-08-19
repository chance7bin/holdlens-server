package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.auth.model.entity.UserAccountEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IUserAccountDao;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertFalse;

public class UserAccountRepositoryTest {

    @Test
    public void duplicateUsernameIsTranslatedToStableInsertFailure() throws Exception {
        IUserAccountDao duplicateDao = (IUserAccountDao) Proxy.newProxyInstance(
                IUserAccountDao.class.getClassLoader(),
                new Class<?>[]{IUserAccountDao.class},
                (proxy, method, args) -> {
                    if ("insert".equals(method.getName())) {
                        throw new DuplicateKeyException("duplicate username");
                    }
                    return null;
                }
        );
        UserAccountRepository repository = new UserAccountRepository();
        Field daoField = UserAccountRepository.class.getDeclaredField("userAccountDao");
        daoField.setAccessible(true);
        daoField.set(repository, duplicateDao);

        assertFalse(repository.insert(UserAccountEntity.register("alice", "password-hash")));
    }
}
