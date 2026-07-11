package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundTopHoldingPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface IFundTopHoldingDao {

    void insert(FundTopHoldingPO fundTopHoldingPO);

    void update(FundTopHoldingPO fundTopHoldingPO);

    void deleteByFundCode(@Param("fundCode") String fundCode);

    void deleteByIds(@Param("ids") Collection<Long> ids);

    FundTopHoldingPO selectById(@Param("id") Long id);

    List<FundTopHoldingPO> selectByFundCodes(@Param("fundCodes") Collection<String> fundCodes);

    List<FundTopHoldingPO> selectByStockCode(@Param("stockCode") String stockCode);

}
