package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.dto.PortfolioFundDetailDTO;
import com.echoamoy.holdlens.server.api.response.Response;

/**
 * 组合资产基金明细查询接口，负责按用户持仓组合返回账户资产与对应的公开基金详情。
 */
public interface IPortfolioFundDetailService {

    /**
     * 查询指定用户的基金类持仓，并关联 server 已保存的最新公开基金明细。
     */
    Response<PortfolioFundDetailDTO> queryPortfolioFundDetails(Long userId);

    /**
     * 查询基金目录中的单只基金详情；缺失或陈旧重仓只触发异步刷新，不阻塞当前响应。
     */
    Response<PortfolioFundDetailDTO.FundDetail> queryFundDetail(String fundCode);

}
