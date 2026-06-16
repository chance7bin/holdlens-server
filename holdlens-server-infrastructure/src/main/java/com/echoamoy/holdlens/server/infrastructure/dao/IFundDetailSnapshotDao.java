package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailSnapshotPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IFundDetailSnapshotDao {

    FundDetailSnapshotPO selectById(@Param("id") Long id);

    List<FundDetailSnapshotPO> selectByUserId(@Param("userId") Long userId);

    List<FundDetailSnapshotPO> selectBySourceRefId(@Param("sourceRefId") String sourceRefId);

}
