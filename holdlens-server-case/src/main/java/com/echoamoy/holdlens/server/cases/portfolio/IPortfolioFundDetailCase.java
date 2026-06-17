package com.echoamoy.holdlens.server.cases.portfolio;

import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;

public interface IPortfolioFundDetailCase {

    PortfolioFundDetailResult queryPortfolioFundDetails(Long userId);

}
