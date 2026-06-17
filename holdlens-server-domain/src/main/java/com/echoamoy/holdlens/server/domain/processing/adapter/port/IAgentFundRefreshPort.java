package com.echoamoy.holdlens.server.domain.processing.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;

public interface IAgentFundRefreshPort {

    FundRefreshDispatchResultEntity dispatch(FundRefreshDispatchCommandEntity commandEntity);

}
