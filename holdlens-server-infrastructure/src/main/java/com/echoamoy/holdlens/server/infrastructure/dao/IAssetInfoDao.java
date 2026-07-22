package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetInfoPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAssetInfoDao {

    void upsertWatchlistAsset(AssetInfoPO assetInfoPO);

    AssetInfoPO selectById(@Param("id") Long id);

    List<AssetInfoPO> selectByUserId(@Param("userId") Long userId);

    AssetInfoPO selectByUserIdAndAssetCodeAndAssetKind(@Param("userId") Long userId,
                                                       @Param("assetCode") String assetCode,
                                                       @Param("assetKind") String assetKind);

    List<AssetInfoPO> selectByUserIdAndAssetName(@Param("userId") Long userId, @Param("assetName") String assetName);

    List<AssetInfoPO> selectEnabledByUserId(@Param("userId") Long userId, @Param("assetKind") String assetKind);

    List<AssetInfoPO> selectWatchlistedByIdentities(@Param("userId") Long userId,
                                                    @Param("identityKeys") java.util.Collection<String> identityKeys);

}
