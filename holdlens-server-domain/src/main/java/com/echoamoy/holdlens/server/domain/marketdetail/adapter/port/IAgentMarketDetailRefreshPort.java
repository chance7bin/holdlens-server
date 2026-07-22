package com.echoamoy.holdlens.server.domain.marketdetail.adapter.port;

import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.MarketDetailDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.valobj.MarketDetailDispatchResultVO;

public interface IAgentMarketDetailRefreshPort {
    MarketDetailDispatchResultVO dispatch(MarketDetailDispatchCommandEntity command);
}
