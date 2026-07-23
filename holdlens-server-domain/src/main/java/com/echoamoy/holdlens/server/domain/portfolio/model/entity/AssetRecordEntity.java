package com.echoamoy.holdlens.server.domain.portfolio.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRecordEntity {

    public static final String KIND_FUND = "FUND";
    public static final String KIND_STOCK = "STOCK";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_DELETED = "DELETED";

    private Long id;
    private Long userId;
    private Long catalogId;
    private String catalogCode;
    private String balanceDirection;
    private String recordName;
    private String assetKind;
    private Long assetId;
    private String assetRef;
    private String assetCode;
    private String assetMarket;
    private BigDecimal amount;
    private String currency;
    private String remark;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public void assignId(Long id) {
        if (this.id != null || id == null || id <= 0) {
            throw new IllegalArgumentException("资产记录ID回填不合法");
        }
        this.id = id;
    }

    public void attachCatalogContext(String catalogCode, String balanceDirection) {
        this.catalogCode = catalogCode;
        this.balanceDirection = AssetCatalogEntity.requireDirection(balanceDirection);
    }

    public void attachAssetIdentity(String assetRef, String assetCode, String assetMarket) {
        this.assetRef = assetRef;
        this.assetCode = assetCode;
        this.assetMarket = assetMarket;
    }

    public static AssetRecordEntity create(Long userId, Long catalogId, String recordName, String assetKind,
                                           Long assetId, BigDecimal amount, String currency, String remark) {
        validateIdentity(assetKind, assetId);
        return AssetRecordEntity.builder()
                .userId(requirePositive(userId, "用户ID"))
                .catalogId(requirePositive(catalogId, "目录ID"))
                .recordName(requireName(recordName))
                .assetKind(assetKind)
                .assetId(assetId)
                .amount(requireAmount(amount))
                .currency(requireCurrency(currency))
                .remark(normalizeRemark(remark))
                .status(STATUS_ACTIVE)
                .build();
    }

    public boolean isConcreteHolding() {
        return assetKind != null && assetId != null;
    }

    public boolean isUnspecifiedInvestment() {
        return assetKind != null && assetId == null;
    }

    public boolean isSplittable() {
        return STATUS_ACTIVE.equals(status) && amount != null && amount.signum() > 0 && isUnspecifiedInvestment();
    }

    public void updateDetails(String recordName, String remark) {
        if (isConcreteHolding() && recordName != null && !this.recordName.equals(recordName.trim())) {
            throw new IllegalStateException("具体持仓名称不可修改");
        }
        if (!isConcreteHolding() && recordName != null) {
            this.recordName = requireName(recordName);
        }
        this.remark = normalizeRemark(remark);
    }

    public BigDecimal updateAmount(BigDecimal newAmount) {
        requireActive();
        BigDecimal before = amount;
        amount = requireAmount(newAmount);
        return before;
    }

    public void archive() {
        requireActive();
        status = STATUS_ARCHIVED;
    }

    public void restore() {
        if (!STATUS_ARCHIVED.equals(status)) {
            throw new IllegalStateException("只有已归档记录可以恢复");
        }
        status = STATUS_ACTIVE;
    }

    public void delete() {
        if (STATUS_DELETED.equals(status)) {
            throw new IllegalStateException("资产记录已删除");
        }
        status = STATUS_DELETED;
    }

    public BigDecimal splitOut(BigDecimal splitAmount) {
        if (!isSplittable()) {
            throw new IllegalStateException("当前资产记录不可拆分");
        }
        BigDecimal value = requirePositiveAmount(splitAmount);
        if (amount.compareTo(value) < 0) {
            throw new IllegalArgumentException("拆分金额超过剩余金额");
        }
        amount = amount.subtract(value);
        if (amount.signum() == 0) {
            status = STATUS_ARCHIVED;
        }
        return value;
    }

    private void requireActive() {
        if (!STATUS_ACTIVE.equals(status)) {
            throw new IllegalStateException("只有活跃资产记录可以修改");
        }
    }

    private static void validateIdentity(String assetKind, Long assetId) {
        if (assetKind == null && assetId != null) {
            throw new IllegalArgumentException("公共资产ID必须同时提供资产类型");
        }
        if (assetKind != null && !KIND_FUND.equals(assetKind) && !KIND_STOCK.equals(assetKind)) {
            throw new IllegalArgumentException("资产类型不合法");
        }
        if (assetId != null && assetId <= 0) {
            throw new IllegalArgumentException("公共资产ID不合法");
        }
    }

    private static Long requirePositive(Long value, String label) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(label + "不合法");
        }
        return value;
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("资产记录名称不能为空");
        }
        return value.trim();
    }

    private static BigDecimal requireAmount(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("资产金额必须为非负数");
        }
        return value;
    }

    private static BigDecimal requirePositiveAmount(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("拆分金额必须大于零");
        }
        return value;
    }

    private static String requireCurrency(String value) {
        if (value == null || !value.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("币种不合法");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeRemark(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
