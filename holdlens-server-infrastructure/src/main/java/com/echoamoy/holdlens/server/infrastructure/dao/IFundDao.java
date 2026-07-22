package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IFundDao {

    void upsertCatalog(FundPO fundPO);

    void upsertCatalogBatch(@Param("funds") List<FundPO> funds);

    int updatePurchaseStatus(FundPO fundPO);

    int updatePeriodReturn(FundPO fundPO);

    int updateTopHoldingMetadata(FundPO fundPO);

    int updateAssetAllocationMetadata(FundPO fundPO);

    int markAssetAllocationUnavailable(@Param("fundCode") String fundCode,
                                       @Param("assetAllocationFetchedAt") java.util.Date assetAllocationFetchedAt);

    int updateLastDetailViewTime(@Param("fundCodes") java.util.Collection<String> fundCodes,
                                 @Param("lastDetailViewTime") java.util.Date lastDetailViewTime);

    FundPO selectById(@Param("id") Long id);

    FundPO selectAssetAllocationMetadataForUpdate(@Param("fundCode") String fundCode);

    List<FundPO> selectByFundCodes(@Param("fundCodes") java.util.Collection<String> fundCodes);

    List<String> selectTopHoldingRefreshTargets(@Param("viewedSince") java.util.Date viewedSince);

    List<String> selectAssetAllocationRefreshTargets(@Param("viewedSince") java.util.Date viewedSince,
                                                     @Param("latestEndedQuarter") java.util.Date latestEndedQuarter,
                                                     @Param("unavailableRetryBefore") java.util.Date unavailableRetryBefore);

    List<FundPO> search(@Param("keyword") String keyword, @Param("limit") int limit);

}
