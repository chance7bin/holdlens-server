package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetCatalogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAssetCatalogDao {
    List<AssetCatalogPO> selectVisible(@Param("userId") Long userId);
    AssetCatalogPO selectVisibleById(@Param("userId") Long userId, @Param("id") Long id);
    AssetCatalogPO selectByCode(@Param("catalogCode") String catalogCode);
    int countEnabledChildren(@Param("userId") Long userId, @Param("parentId") Long parentId);
    void insert(AssetCatalogPO catalog);
    int updateUserCatalog(AssetCatalogPO catalog);
}
