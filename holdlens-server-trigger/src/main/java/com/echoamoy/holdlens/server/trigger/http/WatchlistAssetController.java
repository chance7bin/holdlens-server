package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IWatchlistAssetService;
import com.echoamoy.holdlens.server.api.request.WatchlistAssetBatchAddRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.api.response.WatchlistAssetBatchAddResponseDTO;
import com.echoamoy.holdlens.server.cases.portfolio.IWatchlistAssetBatchAddCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddCommand;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

@RestController
public class WatchlistAssetController implements IWatchlistAssetService {

    @Resource
    private IWatchlistAssetBatchAddCase watchlistAssetBatchAddCase;

    public WatchlistAssetController() {
    }

    public WatchlistAssetController(IWatchlistAssetBatchAddCase watchlistAssetBatchAddCase) {
        this.watchlistAssetBatchAddCase = watchlistAssetBatchAddCase;
    }

    @PostMapping("/api/watchlist/assets/batch-add")
    @Override
    public Response<WatchlistAssetBatchAddResponseDTO> batchAdd(@Valid @RequestBody WatchlistAssetBatchAddRequestDTO request) {
        return Response.ok(toResponseDTO(watchlistAssetBatchAddCase.batchAdd(toCommand(request))));
    }

    @DeleteMapping("/api/watchlist/assets")
    @Override
    public Response<Void> remove(@RequestParam Long userId, @RequestParam String assetKind,
                                 @RequestParam String assetRef) {
        watchlistAssetBatchAddCase.remove(userId, assetKind, assetRef);
        return Response.ok(null);
    }

    private WatchlistAssetBatchAddCommand toCommand(WatchlistAssetBatchAddRequestDTO request) {
        if (request == null) {
            return null;
        }
        return WatchlistAssetBatchAddCommand.builder()
                .userId(request.getUserId())
                .items(toCommandItems(request.getItems()))
                .build();
    }

    private List<WatchlistAssetBatchAddCommand.Item> toCommandItems(List<WatchlistAssetBatchAddRequestDTO.Item> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(item -> item == null ? null : WatchlistAssetBatchAddCommand.Item.builder()
                        .assetKind(item.getAssetKind())
                        .assetRef(item.getAssetRef())
                        .assetCode(item.getAssetCode())
                        .assetName(item.getAssetName())
                        .market(item.getMarket())
                        .build())
                .toList();
    }

    private WatchlistAssetBatchAddResponseDTO toResponseDTO(WatchlistAssetBatchAddResult result) {
        if (result == null) {
            return null;
        }
        return WatchlistAssetBatchAddResponseDTO.builder()
                .invalidItems(toInvalidItems(result.getInvalidItems()))
                .build();
    }

    private List<WatchlistAssetBatchAddResponseDTO.InvalidItem> toInvalidItems(List<WatchlistAssetBatchAddResult.InvalidItem> invalidItems) {
        if (invalidItems == null) {
            return List.of();
        }
        return invalidItems.stream()
                .map(item -> item == null ? null : WatchlistAssetBatchAddResponseDTO.InvalidItem.builder()
                        .index(item.getIndex())
                        .assetKind(item.getAssetKind())
                        .assetRef(item.getAssetRef())
                        .assetCode(item.getAssetCode())
                        .market(item.getMarket())
                        .reasonCode(item.getReasonCode())
                        .reason(item.getReason())
                        .build())
                .toList();
    }

}
