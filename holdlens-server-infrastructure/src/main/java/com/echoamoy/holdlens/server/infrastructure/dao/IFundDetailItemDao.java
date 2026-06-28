package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailItemPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IFundDetailItemDao {

    void upsert(FundDetailItemPO fundDetailItemPO);

    FundDetailItemPO selectById(@Param("id") Long id);

    List<FundDetailItemPO> selectByFundCodes(@Param("fundCodes") java.util.Collection<String> fundCodes);

    List<FundDetailItemPO> selectRefreshTargetsAfterId(@Param("lastId") Long lastId, @Param("limit") int limit);

}
