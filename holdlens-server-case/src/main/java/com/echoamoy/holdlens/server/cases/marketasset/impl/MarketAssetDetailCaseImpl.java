package com.echoamoy.holdlens.server.cases.marketasset.impl;

import com.echoamoy.holdlens.server.cases.marketasset.IMarketAssetDetailCase;
import com.echoamoy.holdlens.server.cases.marketasset.IMarketAssetQueryCase;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetDetailResult;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetQueryResult;
import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDetailCase;
import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailResult;
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
    @Resource private IMarketDetailCase marketDetailCase;

    @Override
    public MarketAssetDetailResult queryDetail(Long userId, String assetKind, String assetRef) {
        return buildDetail(userId, assetKind, assetRef, false);
    }

    @Override
    public MarketAssetDetailResult ensureDetail(Long userId, String assetKind, String assetRef) {
        return buildDetail(userId, assetKind, assetRef, true);
    }

    private MarketAssetDetailResult buildDetail(Long userId, String assetKind, String assetRef, boolean ensure) {
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
            PortfolioFundDetailResult.FundDetail fund = ensure
                    ? portfolioFundDetailCase.ensureFundDetail(ref.getAssetCode())
                    : portfolioFundDetailCase.queryFundDetail(ref.getAssetCode());
            MarketDetailResult.DetailRefresh refresh = ensure
                    ? marketDetailCase.ensureFundDetailData(ref.getAssetCode(), false) : null;
            return MarketAssetDetailResult.builder().assetKind(ref.getAssetKind()).assetRef(ref.value())
                    .watchlisted(watchlisted).fund(fund).refresh(refresh).build();
        }
        MarketAssetQueryResult.StockDetail stock = marketAssetQueryCase.queryStockDetail(userId, ref.value());
        MarketDetailResult.DetailRefresh refresh = ensure
                ? marketDetailCase.ensureStockDetailDataV2(ref.value(), true) : null;
        return MarketAssetDetailResult.builder().assetKind(ref.getAssetKind()).assetRef(ref.value())
                .watchlisted(stock.getWatchlisted()).stock(stock).refresh(refresh).build();
    }

    private AppException illegal(String message) {
        return new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message);
    }
}
