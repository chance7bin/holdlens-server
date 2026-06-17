package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailSnapshotPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IFundDetailSnapshotDao {

    void insert(FundDetailSnapshotPO fundDetailSnapshotPO);

    FundDetailSnapshotPO selectById(@Param("id") Long id);

    List<FundDetailSnapshotPO> selectBySourceRefId(@Param("sourceRefId") String sourceRefId);

}
