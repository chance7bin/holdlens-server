package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundTopHoldingPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IFundTopHoldingDao {

    FundTopHoldingPO selectById(@Param("id") Long id);

    List<FundTopHoldingPO> selectByFundDetailItemId(@Param("fundDetailItemId") Long fundDetailItemId);

    List<FundTopHoldingPO> selectBySnapshotId(@Param("snapshotId") Long snapshotId);

    List<FundTopHoldingPO> selectByStockCode(@Param("stockCode") String stockCode);

}
