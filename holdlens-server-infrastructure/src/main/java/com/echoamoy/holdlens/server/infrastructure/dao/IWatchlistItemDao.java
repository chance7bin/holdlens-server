package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.WatchlistItemPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface IWatchlistItemDao {
    void upsert(WatchlistItemPO item);
    int delete(@Param("userId") Long userId, @Param("assetKind") String assetKind, @Param("assetId") Long assetId);
    WatchlistItemPO selectOneByPublicIdentity(@Param("userId") Long userId,
                                              @Param("assetKind") String assetKind,
                                              @Param("assetCode") String assetCode,
                                              @Param("market") String market);
    List<WatchlistItemPO> selectByUser(@Param("userId") Long userId, @Param("assetKind") String assetKind);
    List<WatchlistItemPO> selectByPublicIdentities(@Param("userId") Long userId,
                                                   @Param("identityKeys") Collection<String> identityKeys);
}
