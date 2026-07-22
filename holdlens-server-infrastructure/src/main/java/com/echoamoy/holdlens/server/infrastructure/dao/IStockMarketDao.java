package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.StockMarketPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface IStockMarketDao {

    void upsert(StockMarketPO stockMarketPO);

    void upsertTarget(StockMarketPO stockMarketPO);

    List<StockMarketPO> selectByStockKeys(@Param("stockKeys") Collection<String> stockKeys);

    List<StockMarketPO> search(@Param("keyword") String keyword, @Param("market") String market,
                               @Param("limit") int limit);

    StockMarketPO selectOne(@Param("stockCode") String stockCode, @Param("market") String market);

}
