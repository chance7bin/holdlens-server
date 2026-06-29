package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.request.WatchlistAssetBatchAddRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.api.response.WatchlistAssetBatchAddResponseDTO;

/**
 * 自选资产接口，只维护用户维度自选关系。
 */
public interface IWatchlistAssetService {

    Response<WatchlistAssetBatchAddResponseDTO> batchAdd(WatchlistAssetBatchAddRequestDTO request);

}
