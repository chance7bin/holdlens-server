package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistItemPO {
    private Long id;
    private Long userId;
    private String assetKind;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String assetType;
    private String market;
    private Date createTime;
}
