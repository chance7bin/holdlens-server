package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.StockMarketCurrentPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface IStockMarketCurrentDao {

    void upsert(StockMarketCurrentPO stockMarketCurrentPO);

    void upsertTarget(StockMarketCurrentPO stockMarketCurrentPO);

    List<StockMarketCurrentPO> selectAllTargets();

    List<StockMarketCurrentPO> selectByStockKeys(@Param("stockKeys") Collection<String> stockKeys);

}
