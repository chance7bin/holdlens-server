package com.echoamoy.holdlens.server.domain.processing.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.StockQuoteRefreshDispatchCommandEntity;

public interface IAgentStockQuoteRefreshPort {

    FundRefreshDispatchResultEntity dispatch(StockQuoteRefreshDispatchCommandEntity commandEntity);

}
