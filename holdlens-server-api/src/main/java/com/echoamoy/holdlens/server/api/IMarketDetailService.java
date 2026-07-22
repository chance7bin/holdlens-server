package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.dto.MarketDetailDTO;
import com.echoamoy.holdlens.server.api.request.MarketDetailRefreshRequest;
import com.echoamoy.holdlens.server.api.response.Response;

public interface IMarketDetailService {
    Response<MarketDetailDTO.Task> createTask(MarketDetailRefreshRequest.Create request);
    Response<MarketDetailDTO.Task> callback(String callbackHeader, String idempotencyHeader,
                                             MarketDetailRefreshRequest.Callback request);
    Response<MarketDetailDTO.FundNavHistory> queryFundNavHistory(String fundCode, String period);
    Response<MarketDetailDTO.StockPriceHistory> queryStockPriceHistory(String assetRef, String period);
    Response<MarketDetailDTO.StockCompanyProfile> queryStockCompanyProfile(String assetRef);
}
