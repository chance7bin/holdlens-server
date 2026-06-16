package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailItemPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IFundDetailItemDao {

    FundDetailItemPO selectById(@Param("id") Long id);

    List<FundDetailItemPO> selectBySnapshotId(@Param("snapshotId") Long snapshotId);

    FundDetailItemPO selectByUserIdAndFundCode(@Param("userId") Long userId, @Param("fundCode") String fundCode);

}
