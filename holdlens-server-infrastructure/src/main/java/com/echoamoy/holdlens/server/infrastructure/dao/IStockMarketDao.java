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

}
