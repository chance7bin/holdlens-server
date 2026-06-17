package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailItemPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IFundDetailItemDao {

    void insert(FundDetailItemPO fundDetailItemPO);

    FundDetailItemPO selectById(@Param("id") Long id);

    List<FundDetailItemPO> selectBySnapshotId(@Param("snapshotId") Long snapshotId);

    List<FundDetailItemPO> selectLatestByFundCodes(@Param("fundCodes") java.util.Collection<String> fundCodes);

}
