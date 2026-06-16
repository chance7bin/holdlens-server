package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetAccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAssetAccountDao {

    AssetAccountPO selectById(@Param("id") Long id);

    List<AssetAccountPO> selectByUserId(@Param("userId") Long userId);

    AssetAccountPO selectByUserIdAndAccountName(@Param("userId") Long userId, @Param("accountName") String accountName);

}
