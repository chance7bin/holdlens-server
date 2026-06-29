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
public class WatchlistAssetBatchAddResult {

    private List<InvalidItem> invalidItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvalidItem {

        /** 0 基请求数组下标。 */
        private Integer index;

        private String assetKind;

        private String assetCode;

        private String market;

        private String reasonCode;

        private String reason;

    }

}
