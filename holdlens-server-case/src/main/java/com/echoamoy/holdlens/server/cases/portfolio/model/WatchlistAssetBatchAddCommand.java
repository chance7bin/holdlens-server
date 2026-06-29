package com.echoamoy.holdlens.server.cases.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistAssetBatchAddCommand {

    private Long userId;

    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        private String assetKind;

        private String assetCode;

        private String assetName;

        private String market;

    }

}
