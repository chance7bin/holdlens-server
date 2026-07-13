package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.dto.FundRefreshTaskDTO;
import com.echoamoy.holdlens.server.api.request.FundSliceRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.response.Response;

public interface IAgentFundSliceRefreshService {
    Response<FundRefreshTaskDTO> catalogCallback(String callbackHeader, FundSliceRefreshCallbackRequest request);
    Response<FundRefreshTaskDTO> purchaseStatusCallback(String callbackHeader, FundSliceRefreshCallbackRequest request);
    Response<FundRefreshTaskDTO> periodReturnCallback(String callbackHeader, FundSliceRefreshCallbackRequest request);
    Response<FundRefreshTaskDTO> topHoldingCallback(String callbackHeader, FundSliceRefreshCallbackRequest request);
}
