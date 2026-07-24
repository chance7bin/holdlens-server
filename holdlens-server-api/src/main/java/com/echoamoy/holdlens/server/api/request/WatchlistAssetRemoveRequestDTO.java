package com.echoamoy.holdlens.server.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistAssetRemoveRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private Long userId;

    @NotBlank
    private String assetKind;

    @NotBlank
    private String assetRef;

}
