package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.UserSessionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IUserSessionDao {

    int insert(UserSessionPO session);

    UserSessionPO selectByTokenHash(@Param("tokenHash") String tokenHash);

    int revoke(@Param("id") Long id);
}
