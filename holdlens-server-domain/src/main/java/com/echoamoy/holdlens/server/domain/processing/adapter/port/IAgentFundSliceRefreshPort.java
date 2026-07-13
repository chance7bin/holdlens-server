package com.echoamoy.holdlens.server.domain.processing.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundSliceRefreshDispatchCommandEntity;

public interface IAgentFundSliceRefreshPort {
    FundRefreshDispatchResultEntity dispatch(FundSliceRefreshDispatchCommandEntity commandEntity);
}
