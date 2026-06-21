package com.echoamoy.holdlens.server.domain.funddata.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IFundDataRepository {

    void saveCurrentData(FundCurrentDataAggregate aggregate);

    Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes);

}
