package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingCallbackPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IProcessingCallbackDao {

    void insert(ProcessingCallbackPO processingCallbackPO);

    int updateProcessStatus(@Param("serverTaskId") String serverTaskId,
                            @Param("idempotencyKey") String idempotencyKey,
                            @Param("processStatus") String processStatus,
                            @Param("errorSummary") String errorSummary);

}
