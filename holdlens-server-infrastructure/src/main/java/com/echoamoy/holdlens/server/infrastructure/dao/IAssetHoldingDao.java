package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetHoldingPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAssetHoldingDao {

    AssetHoldingPO selectById(@Param("id") Long id);

    List<AssetHoldingPO> selectByUserId(@Param("userId") Long userId);

    AssetHoldingPO selectByUserIdAndAccountIdAndAssetId(@Param("userId") Long userId,
                                                        @Param("accountId") Long accountId,
                                                        @Param("assetId") Long assetId);

    List<AssetHoldingPO> selectByAssetId(@Param("assetId") Long assetId);

}
