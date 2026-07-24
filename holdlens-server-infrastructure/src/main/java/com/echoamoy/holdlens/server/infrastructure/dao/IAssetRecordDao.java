package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAssetRecordDao {
    void insert(AssetRecordPO record);
    AssetRecordPO selectByUserAndId(@Param("userId") Long userId, @Param("id") Long id);
    AssetRecordPO selectActiveByUserAndId(@Param("userId") Long userId, @Param("id") Long id);
    AssetRecordPO selectByUserAndIdForUpdate(@Param("userId") Long userId, @Param("id") Long id);
    List<AssetRecordPO> selectActiveByUserId(@Param("userId") Long userId);
    List<AssetRecordPO> selectActiveByUserAndAssetRef(@Param("userId") Long userId,
                                                      @Param("assetRef") String assetRef);
    int countActiveByCatalog(@Param("userId") Long userId, @Param("catalogId") Long catalogId);
    int updateMutable(AssetRecordPO record);
}
