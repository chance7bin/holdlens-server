package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IProcessingLogDao {

    void insert(ProcessingLogPO processingLogPO);

    ProcessingLogPO selectById(@Param("id") Long id);

    List<ProcessingLogPO> selectBySourceRefId(@Param("sourceRefId") String sourceRefId);

}
