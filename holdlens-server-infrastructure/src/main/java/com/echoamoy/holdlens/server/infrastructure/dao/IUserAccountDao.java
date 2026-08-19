package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.UserAccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IUserAccountDao {

    UserAccountPO selectByUsername(@Param("username") String username);

    UserAccountPO selectByUsernameForUpdate(@Param("username") String username);

    UserAccountPO selectById(@Param("id") Long id);

    int insert(UserAccountPO account);

    int updateLoginState(UserAccountPO account);
}
