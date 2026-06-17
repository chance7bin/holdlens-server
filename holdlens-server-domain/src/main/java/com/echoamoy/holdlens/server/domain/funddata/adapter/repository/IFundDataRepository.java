package com.echoamoy.holdlens.server.domain.funddata.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundDetailSnapshotAggregate;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IFundDataRepository {

    Long saveSnapshot(FundDetailSnapshotAggregate aggregate);

    Map<String, FundDetailSnapshotAggregate.FundDetail> queryLatestDetails(Set<String> fundCodes);

}
