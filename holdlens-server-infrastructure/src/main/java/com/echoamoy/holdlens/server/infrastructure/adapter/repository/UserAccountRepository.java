package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.auth.adapter.repository.IUserAccountRepository;
import com.echoamoy.holdlens.server.domain.auth.model.entity.UserAccountEntity;
import com.echoamoy.holdlens.server.domain.auth.model.valobj.UserAccountStatusEnumVO;
import com.echoamoy.holdlens.server.infrastructure.dao.IUserAccountDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.UserAccountPO;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Repository
public class UserAccountRepository implements IUserAccountRepository {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private IUserAccountDao userAccountDao;

    @Override
    public UserAccountEntity findByUsername(String username) {
        return toEntity(userAccountDao.selectByUsername(username));
    }

    @Override
    public UserAccountEntity findByUsernameForUpdate(String username) {
        return toEntity(userAccountDao.selectByUsernameForUpdate(username));
    }

    @Override
    public UserAccountEntity findById(Long userId) {
        return toEntity(userAccountDao.selectById(userId));
    }

    @Override
    public boolean insert(UserAccountEntity account) {
        UserAccountPO po = toPO(account);
        boolean inserted;
        try {
            inserted = userAccountDao.insert(po) == 1;
        } catch (DuplicateKeyException exception) {
            return false;
        }
        if (inserted) {
            account.restore(po.getId(), account.getUsername(), account.getPasswordHash(), account.getStatus(),
                    account.getFailedLoginCount(), account.getLockedUntil(), account.getCreateTime(), account.getUpdateTime());
        }
        return inserted;
    }

    @Override
    public void updateLoginState(UserAccountEntity account) {
        if (userAccountDao.updateLoginState(toPO(account)) != 1) {
            throw new IllegalArgumentException("账号不存在或不可登录");
        }
    }

    private UserAccountPO toPO(UserAccountEntity account) {
        return UserAccountPO.builder()
                .id(account.getId())
                .username(account.getUsername())
                .passwordHash(account.getPasswordHash())
                .status(account.getStatus().name())
                .failedLoginCount(account.getFailedLoginCount())
                .lockedUntil(toDate(account.getLockedUntil()))
                .build();
    }

    private UserAccountEntity toEntity(UserAccountPO po) {
        if (po == null) {
            return null;
        }
        UserAccountEntity account = new UserAccountEntity();
        account.restore(po.getId(), po.getUsername(), po.getPasswordHash(), UserAccountStatusEnumVO.valueOf(po.getStatus()),
                po.getFailedLoginCount(), toLocalDateTime(po.getLockedUntil()), toLocalDateTime(po.getCreateTime()),
                toLocalDateTime(po.getUpdateTime()));
        return account;
    }

    private Date toDate(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(ZONE).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZONE);
    }
}
