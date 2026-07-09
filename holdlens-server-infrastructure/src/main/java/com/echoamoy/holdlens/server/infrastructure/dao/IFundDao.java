package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IFundDao {

    void upsert(FundPO fundPO);

    void upsertTarget(FundPO fundPO);

    FundPO selectById(@Param("id") Long id);

    List<FundPO> selectByFundCodes(@Param("fundCodes") java.util.Collection<String> fundCodes);

    List<FundPO> selectRefreshTargetsAfterId(@Param("lastId") Long lastId, @Param("limit") int limit);

}
