package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.AgentWarningPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAgentWarningDao {

    AgentWarningPO selectById(@Param("id") Long id);

    List<AgentWarningPO> selectByUserId(@Param("userId") Long userId);

    List<AgentWarningPO> selectBySnapshotId(@Param("snapshotId") Long snapshotId);

    List<AgentWarningPO> selectByFundCode(@Param("fundCode") String fundCode);

    List<AgentWarningPO> selectBySourceRefId(@Param("sourceRefId") String sourceRefId);

}
