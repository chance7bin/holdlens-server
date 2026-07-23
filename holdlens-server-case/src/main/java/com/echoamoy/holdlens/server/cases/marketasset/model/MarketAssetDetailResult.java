package com.echoamoy.holdlens.server.cases.marketasset.model;

import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketAssetDetailResult {
    private String assetKind;
    private String assetRef;
    private Boolean watchlisted;
    private PortfolioFundDetailResult.FundDetail fund;
    private MarketAssetQueryResult.StockDetail stock;
}
