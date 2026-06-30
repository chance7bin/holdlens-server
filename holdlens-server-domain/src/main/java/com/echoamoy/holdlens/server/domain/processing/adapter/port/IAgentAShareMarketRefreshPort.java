package com.echoamoy.holdlens.server.domain.processing.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.model.entity.AShareMarketRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;

public interface IAgentAShareMarketRefreshPort {

    FundRefreshDispatchResultEntity dispatch(AShareMarketRefreshDispatchCommandEntity commandEntity);

}
