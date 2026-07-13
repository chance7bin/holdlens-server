package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IFundDao {

    void upsertCatalog(FundPO fundPO);

    int updatePurchaseStatus(FundPO fundPO);

    int updatePeriodReturn(FundPO fundPO);

    int updateTopHoldingMetadata(FundPO fundPO);

    int updateLastDetailViewTime(@Param("fundCodes") java.util.Collection<String> fundCodes,
                                 @Param("lastDetailViewTime") java.util.Date lastDetailViewTime);

    FundPO selectById(@Param("id") Long id);

    List<FundPO> selectByFundCodes(@Param("fundCodes") java.util.Collection<String> fundCodes);

    List<String> selectTopHoldingRefreshTargets(@Param("viewedSince") java.util.Date viewedSince);

}
