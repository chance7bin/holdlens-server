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

    default void upsertCatalog(FundCurrentDataAggregate.FundDetail fund) { throw unsupported("upsertCatalog"); }

    void upsertCatalogs(List<FundCurrentDataAggregate.FundDetail> funds);

    default boolean updatePurchaseStatus(FundCurrentDataAggregate.FundDetail fund) { throw unsupported("updatePurchaseStatus"); }

    default boolean updateTopHoldingSnapshot(FundCurrentDataAggregate.FundDetail fund, boolean clearHoldings) { throw unsupported("updateTopHoldingSnapshot"); }

    default List<String> queryTopHoldingRefreshTargets(LocalDateTime viewedSince) { throw unsupported("queryTopHoldingRefreshTargets"); }

    default List<String> queryTopHoldingRefreshTargets(LocalDateTime viewedSince, LocalDateTime staleBefore) {
        throw unsupported("queryTopHoldingRefreshTargetsWithStaleBefore");
    }

    default List<String> queryAssetAllocationRefreshTargets(LocalDateTime viewedSince, LocalDate latestEndedQuarter,
                                                            LocalDateTime unavailableRetryBefore) { throw unsupported("queryAssetAllocationRefreshTargets"); }

    default boolean replaceAssetAllocationSnapshot(FundCurrentDataAggregate.FundDetail fund) { throw unsupported("replaceAssetAllocationSnapshot"); }

    default boolean markAssetAllocationUnavailable(String fundCode, LocalDateTime fetchedAt) { throw unsupported("markAssetAllocationUnavailable"); }

    default void markDetailViewed(Collection<String> fundCodes, LocalDateTime viewedAt) { throw unsupported("markDetailViewed"); }

    default void markDetailViewed(Collection<String> fundCodes, LocalDateTime viewedAt,
                                  LocalDateTime updateBefore) {
        throw unsupported("markDetailViewedWithThrottle");
    }

    default List<String> queryDetailRefreshTargets(LocalDateTime viewedSince, int limit) {
        throw unsupported("queryDetailRefreshTargets");
    }

    default List<FundCurrentDataAggregate.FundDetail> search(String keyword, int limit) { throw unsupported("search"); }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException("IFundDataRepository must implement " + operation);
    }

}
