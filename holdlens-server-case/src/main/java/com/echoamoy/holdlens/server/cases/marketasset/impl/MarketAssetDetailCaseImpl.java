package com.echoamoy.holdlens.server.cases.marketasset.impl;

import com.echoamoy.holdlens.server.cases.marketasset.IMarketAssetDetailCase;
import com.echoamoy.holdlens.server.cases.marketasset.IMarketAssetQueryCase;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetDetailResult;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetQueryResult;
import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.domain.marketasset.model.valobj.MarketAssetRefVO;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class MarketAssetDetailCaseImpl implements IMarketAssetDetailCase {

    @Resource private IPortfolioFundDetailCase portfolioFundDetailCase;
    @Resource private IMarketAssetQueryCase marketAssetQueryCase;
    @Resource private IPortfolioRepository portfolioRepository;

    @Override
    public MarketAssetDetailResult queryDetail(Long userId, String assetKind, String assetRef) {
        if (userId == null || userId <= 0) {
            throw illegal("用户ID不合法");
        }
        MarketAssetRefVO ref;
        try {
            ref = MarketAssetRefVO.parse(assetKind, assetRef);
        } catch (IllegalArgumentException exception) {
            throw illegal(exception.getMessage());
        }
        if (MarketAssetRefVO.KIND_FUND.equals(ref.getAssetKind())) {
            boolean watchlisted = portfolioRepository.queryWatchlistAsset(
                    userId, ref.getAssetCode(), ref.getAssetKind()) != null;
            return MarketAssetDetailResult.builder().assetKind(ref.getAssetKind()).assetRef(ref.value())
                    .watchlisted(watchlisted).fund(portfolioFundDetailCase.queryFundDetail(ref.getAssetCode())).build();
        }
        MarketAssetQueryResult.StockDetail stock = marketAssetQueryCase.queryStockDetail(userId, ref.value());
        return MarketAssetDetailResult.builder().assetKind(ref.getAssetKind()).assetRef(ref.value())
                .watchlisted(stock.getWatchlisted()).stock(stock).build();
    }

    private AppException illegal(String message) {
        return new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message);
    }
}
