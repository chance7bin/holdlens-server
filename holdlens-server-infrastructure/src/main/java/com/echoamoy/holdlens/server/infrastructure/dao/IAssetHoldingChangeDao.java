package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetHoldingChangePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAssetHoldingChangeDao {

    AssetHoldingChangePO selectById(@Param("id") Long id);

    List<AssetHoldingChangePO> selectByUserId(@Param("userId") Long userId);

    List<AssetHoldingChangePO> selectByHoldingId(@Param("holdingId") Long holdingId);

    List<AssetHoldingChangePO> selectByAssetId(@Param("assetId") Long assetId);

    List<AssetHoldingChangePO> selectBySourceRefId(@Param("sourceRefId") String sourceRefId);

}
