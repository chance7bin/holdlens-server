package com.echoamoy.holdlens.server.domain.portfolio.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCatalogEntity {

    public static final String SCOPE_SYSTEM = "SYSTEM";
    public static final String SCOPE_USER = "USER";
    public static final String DIRECTION_ADD = "ADD";
    public static final String DIRECTION_SUBTRACT = "SUBTRACT";
    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DELETED = "DELETED";
    public static final String CODE_INVESTMENT_ASSET = "INVESTMENT_ASSET";
    public static final String CODE_FUND = "FUND";
    public static final String CODE_STOCK = "STOCK";

    private Long id;
    private Long userId;
    private Long parentId;
    private String catalogCode;
    private String catalogName;
    private String catalogScope;
    private String balanceDirection;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AssetCatalogEntity createUser(Long userId, Long parentId, String catalogName,
                                                String balanceDirection, Integer sortOrder) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID不合法");
        }
        return AssetCatalogEntity.builder()
                .userId(userId)
                .parentId(parentId)
                .catalogName(requireName(catalogName))
                .catalogScope(SCOPE_USER)
                .balanceDirection(requireDirection(balanceDirection))
                .sortOrder(sortOrder == null ? 0 : sortOrder)
                .status(STATUS_ENABLED)
                .build();
    }

    public void assignId(Long id) {
        if (this.id != null || id == null || id <= 0) {
            throw new IllegalArgumentException("目录ID回填不合法");
        }
        this.id = id;
    }

    public boolean isSystem() {
        return SCOPE_SYSTEM.equals(catalogScope);
    }

    public boolean isEnabled() {
        return STATUS_ENABLED.equals(status);
    }

    public boolean isFundCatalog() {
        return CODE_FUND.equals(catalogCode);
    }

    public boolean isStockCatalog() {
        return CODE_STOCK.equals(catalogCode);
    }

    public void updateUserCatalog(Long parentId, String catalogName, String balanceDirection, Integer sortOrder) {
        if (isSystem()) {
            throw new IllegalStateException("系统目录不可修改");
        }
        this.parentId = parentId;
        this.catalogName = requireName(catalogName);
        this.balanceDirection = requireDirection(balanceDirection);
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public void markDeleted() {
        if (isSystem()) {
            throw new IllegalStateException("系统目录不可删除");
        }
        this.status = STATUS_DELETED;
    }

    public static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("目录名称不能为空");
        }
        return value.trim();
    }

    public static String requireDirection(String value) {
        if (!DIRECTION_ADD.equals(value) && !DIRECTION_SUBTRACT.equals(value)) {
            throw new IllegalArgumentException("目录金额方向不合法");
        }
        return value;
    }
}
