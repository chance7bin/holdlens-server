package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.auth.adapter.repository.IUserSessionRepository;
import com.echoamoy.holdlens.server.domain.auth.model.entity.UserSessionEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IUserSessionDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.UserSessionPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Repository
public class UserSessionRepository implements IUserSessionRepository {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private IUserSessionDao userSessionDao;

    @Override
    public void insert(UserSessionEntity session) {
        UserSessionPO po = toPO(session);
        if (userSessionDao.insert(po) != 1) {
            throw new IllegalStateException("创建会话失败");
        }
        session.restore(po.getId(), session.getUserId(), session.getTokenHash(), session.getExpiresAt(),
                session.getRevokedAt(), session.getCreateTime());
    }

    @Override
    public UserSessionEntity findByTokenHash(String tokenHash) {
        return toEntity(userSessionDao.selectByTokenHash(tokenHash));
    }

    @Override
    public UserSessionEntity findByIdForUpdate(Long sessionId) {
        return toEntity(userSessionDao.selectByIdForUpdate(sessionId));
    }

    @Override
    public void revokeActiveByUserId(Long userId) {
        userSessionDao.revokeActiveByUserId(userId);
    }

    @Override
    public void revoke(Long sessionId) {
        userSessionDao.revoke(sessionId);
    }

    @Override
    public boolean updateExpiresAt(UserSessionEntity session) {
        return userSessionDao.updateExpiresAt(session.getId(), toDate(session.getExpiresAt())) == 1;
    }

    private UserSessionPO toPO(UserSessionEntity session) {
        return UserSessionPO.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .tokenHash(session.getTokenHash())
                .expiresAt(toDate(session.getExpiresAt()))
                .revokedAt(toDate(session.getRevokedAt()))
                .build();
    }

    private UserSessionEntity toEntity(UserSessionPO po) {
        if (po == null) {
            return null;
        }
        UserSessionEntity session = new UserSessionEntity();
        session.restore(po.getId(), po.getUserId(), po.getTokenHash(), toLocalDateTime(po.getExpiresAt()),
                toLocalDateTime(po.getRevokedAt()), toLocalDateTime(po.getCreateTime()));
        return session;
    }

    private Date toDate(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(ZONE).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZONE);
    }
}
