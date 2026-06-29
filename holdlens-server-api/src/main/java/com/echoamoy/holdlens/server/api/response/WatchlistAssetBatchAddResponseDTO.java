package com.echoamoy.holdlens.server.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistAssetBatchAddResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<InvalidItem> invalidItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvalidItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 0 基请求数组下标。 */
        private Integer index;

        private String assetKind;

        private String assetCode;

        private String market;

        private String reasonCode;

        private String reason;

    }

}
