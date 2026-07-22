package com.echoamoy.holdlens.server.domain.marketasset.model.valobj;

import java.util.Locale;
import java.util.Objects;

/**
 * 市场资产 API 引用。引用不持久化，客户端只应把它作为不透明字符串传递。
 */
public final class MarketAssetRefVO {

    public static final String KIND_FUND = "fund";
    public static final String KIND_STOCK = "stock";
    public static final String MARKET_A_SHARE = "A_SHARE";
    public static final String MARKET_US_STOCK = "US_STOCK";

    private final String assetKind;
    private final String assetCode;
    private final String market;

    private MarketAssetRefVO(String assetKind, String assetCode, String market) {
        this.assetKind = assetKind;
        this.assetCode = assetCode;
        this.market = market;
    }

    public static MarketAssetRefVO fund(String fundCode) {
        return new MarketAssetRefVO(KIND_FUND, requireToken(fundCode, "基金代码"), null);
    }

    public static MarketAssetRefVO stock(String market, String stockCode) {
        String normalizedMarket = normalizeMarket(market);
        return new MarketAssetRefVO(KIND_STOCK, requireToken(stockCode, "股票代码"), normalizedMarket);
    }

    public static MarketAssetRefVO parse(String assetRef) {
        if (assetRef == null || assetRef.isBlank()) {
            throw new IllegalArgumentException("assetRef 不能为空");
        }
        String[] parts = assetRef.trim().split(":", -1);
        if (parts.length == 2 && KIND_FUND.equals(parts[0])) {
            return fund(parts[1]);
        }
        if (parts.length == 3 && KIND_STOCK.equals(parts[0])) {
            return stock(parts[1], parts[2]);
        }
        throw new IllegalArgumentException("assetRef 格式不合法");
    }

    public static MarketAssetRefVO parse(String expectedKind, String assetRef) {
        MarketAssetRefVO parsed = parse(assetRef);
        String normalizedKind = expectedKind == null ? null : expectedKind.trim().toLowerCase(Locale.ROOT);
        if (!Objects.equals(normalizedKind, parsed.assetKind)) {
            throw new IllegalArgumentException("assetKind 与 assetRef 不一致");
        }
        return parsed;
    }

    public String value() {
        return KIND_FUND.equals(assetKind)
                ? KIND_FUND + ":" + assetCode
                : KIND_STOCK + ":" + market + ":" + assetCode;
    }

    public String getAssetKind() {
        return assetKind;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public String getMarket() {
        return market;
    }

    private static String requireToken(String value, String label) {
        if (value == null || value.isBlank() || value.indexOf(':') >= 0) {
            throw new IllegalArgumentException(label + "不合法");
        }
        return value.trim();
    }

    private static String normalizeMarket(String market) {
        String value = requireToken(market, "股票市场").toUpperCase(Locale.ROOT);
        if (!MARKET_A_SHARE.equals(value) && !MARKET_US_STOCK.equals(value)) {
            throw new IllegalArgumentException("股票市场不支持");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MarketAssetRefVO that)) return false;
        return Objects.equals(assetKind, that.assetKind)
                && Objects.equals(assetCode, that.assetCode)
                && Objects.equals(market, that.market);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetKind, assetCode, market);
    }

    @Override
    public String toString() {
        return value();
    }
}
