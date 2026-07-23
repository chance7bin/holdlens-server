package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IPortfolioFundDetailService;
import com.echoamoy.holdlens.server.api.dto.FundDetailDTO;
import com.echoamoy.holdlens.server.api.dto.PortfolioFundDetailDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
public class PortfolioFundDetailController implements IPortfolioFundDetailService {

    @Resource
    private IPortfolioFundDetailCase portfolioFundDetailCase;

    @GetMapping("/api/portfolio/assets/fund-details")
    @Override
    public Response<PortfolioFundDetailDTO> queryPortfolioFundDetails(@RequestParam("userId") Long userId) {
        return Response.ok(toDTO(portfolioFundDetailCase.queryPortfolioFundDetails(userId)));
    }

    @GetMapping("/api/funds/{fundCode}")
    @Override
    public Response<FundDetailDTO> queryFundDetail(@PathVariable("fundCode") String fundCode) {
        return Response.ok(FundDetailDtoMapper.toDTO(portfolioFundDetailCase.queryFundDetail(fundCode)));
    }

    private PortfolioFundDetailDTO toDTO(PortfolioFundDetailResult result) {
        if (result == null) {
            return null;
        }
        return PortfolioFundDetailDTO.builder()
                .userId(result.getUserId())
                .holdings(toHoldingDetails(result.getHoldings()))
                .build();
    }

    private List<PortfolioFundDetailDTO.HoldingDetail> toHoldingDetails(List<PortfolioFundDetailResult.HoldingDetail> holdings) {
        if (holdings == null) {
            return null;
        }
        return holdings.stream()
                .map(holding -> holding == null ? null : PortfolioFundDetailDTO.HoldingDetail.builder()
                        .recordId(holding.getRecordId())
                        .assetRef(holding.getAssetRef())
                        .assetCode(holding.getAssetCode())
                        .assetName(holding.getAssetName())
                        .assetKind(holding.getAssetKind())
                        .assetType(holding.getAssetType())
                        .amount(holding.getAmount())
                        .currency(holding.getCurrency())
                        .status(holding.getStatus())
                        .fundDetail(FundDetailDtoMapper.toDTO(holding.getFundDetail()))
                        .build())
                .toList();
    }

}
