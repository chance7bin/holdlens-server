package com.echoamoy.holdlens.server.domain.funddata.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.funddata.model.entity.FundRefreshTargetEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IFundDataRepository {

    void saveCurrentData(FundCurrentDataAggregate aggregate);

    Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes);

    Set<String> queryExistingFundCodes(Collection<String> fundCodes);

    void registerRefreshTargets(List<FundRefreshTargetEntity> refreshTargets);

    List<FundRefreshTargetEntity> queryRefreshTargetsAfterId(Long lastId, int limit);

}
