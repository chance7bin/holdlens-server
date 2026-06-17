package com.echoamoy.holdlens.server.domain.portfolio.adapter.repository;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;

import java.util.List;

public interface IPortfolioRepository {

    List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId);

}
