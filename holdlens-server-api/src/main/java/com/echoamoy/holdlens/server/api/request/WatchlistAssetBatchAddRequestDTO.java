package com.echoamoy.holdlens.server.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class WatchlistAssetBatchAddRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private Long userId;

    @Valid
    @NotEmpty
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        private String assetKind;

        private String assetCode;

        private String assetName;

        private String market;

    }

}
