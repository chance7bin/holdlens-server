package com.echoamoy.holdlens.server.domain.portfolio.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户自选资产。只表达用户已将某只公开基金或股票加入自选，不代表用户已经持有该资产。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistAssetEntity {

    private Long id;

    private Long userId;

    private String assetCode;

    private String assetName;

    private String assetKind;

    private String assetType;

    private String market;

    private String status;

}
