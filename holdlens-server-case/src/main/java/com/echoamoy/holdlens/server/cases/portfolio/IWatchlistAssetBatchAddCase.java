package com.echoamoy.holdlens.server.cases.portfolio;

import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddCommand;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddResult;

/**
 * 自选资产批量添加用例。只维护用户自选关系，不创建持仓或刷新任务。
 */
public interface IWatchlistAssetBatchAddCase {

    WatchlistAssetBatchAddResult batchAdd(WatchlistAssetBatchAddCommand command);

    default void remove(Long userId, String assetKind, String assetRef) {
        throw new UnsupportedOperationException();
    }

}
