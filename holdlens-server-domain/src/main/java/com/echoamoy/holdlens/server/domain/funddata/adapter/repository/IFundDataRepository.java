package com.echoamoy.holdlens.server.domain.funddata.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;

import java.util.Collection;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IFundDataRepository {

    Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes);

    Set<String> queryExistingFundCodes(Collection<String> fundCodes);

    default void upsertCatalog(FundCurrentDataAggregate.FundDetail fund) { throw new UnsupportedOperationException(); }

    void upsertCatalogs(List<FundCurrentDataAggregate.FundDetail> funds);

    default boolean updatePurchaseStatus(FundCurrentDataAggregate.FundDetail fund) { return false; }

    default boolean updatePeriodReturn(FundCurrentDataAggregate.FundDetail fund) { return false; }

    default boolean updateTopHoldingSnapshot(FundCurrentDataAggregate.FundDetail fund, boolean clearHoldings) { return false; }

    default List<String> queryTopHoldingRefreshTargets(LocalDateTime viewedSince) { return List.of(); }

    default List<String> queryAssetAllocationRefreshTargets(LocalDateTime viewedSince, LocalDate latestEndedQuarter,
                                                            LocalDateTime unavailableRetryBefore) { return List.of(); }

    default boolean replaceAssetAllocationSnapshot(FundCurrentDataAggregate.FundDetail fund) { return false; }

    default boolean markAssetAllocationUnavailable(String fundCode, LocalDateTime fetchedAt) { return false; }

    default void markDetailViewed(Collection<String> fundCodes, LocalDateTime viewedAt) { }

}
