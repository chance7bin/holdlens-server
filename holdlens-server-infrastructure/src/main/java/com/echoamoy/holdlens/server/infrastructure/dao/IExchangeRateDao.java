package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.ExchangeRatePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface IExchangeRateDao {
    void upsert(ExchangeRatePO rate);
    ExchangeRatePO selectOne(@Param("baseCurrency") String baseCurrency, @Param("quoteCurrency") String quoteCurrency);
    List<ExchangeRatePO> selectByBases(@Param("baseCurrencies") Collection<String> baseCurrencies,
                                       @Param("quoteCurrency") String quoteCurrency);
}
