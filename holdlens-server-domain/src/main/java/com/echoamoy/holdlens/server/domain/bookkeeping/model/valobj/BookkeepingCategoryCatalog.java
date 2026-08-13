package com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj;

import java.util.List;
import java.util.Map;

/**
 * 服务端图标白名单。图标资源由客户端发布，双方通过稳定 iconKey 对齐。
 */
public final class BookkeepingCategoryCatalog {

    public static final List<String> ICONS = List.of(
            "food", "vegetable", "fruit", "snack", "milk-tea", "transport",
            "taxi", "public-transit", "fuel", "parking", "train-ticket", "airline-ticket",
            "housing", "home", "utilities", "renovation", "repair", "home-appliance",
            "shopping", "clothing", "beauty", "haircare", "electronics", "maternity-baby",
            "medical", "doctor-visit", "medicine", "sport", "wellness", "entertainment",
            "gaming", "concert", "travel", "pet", "training", "tuition", "stationery",
            "educational-material", "gift", "charity", "insurance", "salary", "bonus",
            "part-time", "business", "investment-income", "reimbursement", "other-expense",
            "other-income", "daily", "communication", "tobacco-alcohol", "flowers", "top-up",
            "credit-card", "transfer", "transaction-details", "bookkeeping", "statistics",
            "settings", "edit", "performance", "airline-ticket-alt-1", "airline-ticket-alt-2",
            "concert-alt-1", "concert-alt-2", "stationery-alt-1", "stationery-alt-2"
    );

    public static final Map<String, List<String>> GROUPS = Map.ofEntries(
            Map.entry("food", List.of(
                    "food", "vegetable", "fruit", "snack", "milk-tea", "tobacco-alcohol"
            )),
            Map.entry("transport", List.of(
                    "transport", "public-transit", "taxi", "parking", "fuel", "train-ticket",
                    "airline-ticket", "airline-ticket-alt-1", "airline-ticket-alt-2", "travel"
            )),
            Map.entry("home", List.of(
                    "daily", "home", "housing", "utilities", "renovation", "repair",
                    "home-appliance", "pet"
            )),
            Map.entry("shopping", List.of(
                    "shopping", "clothing", "beauty", "haircare", "electronics"
            )),
            Map.entry("health", List.of(
                    "medical", "doctor-visit", "medicine", "wellness", "sport", "training",
                    "maternity-baby"
            )),
            Map.entry("entertainment", List.of(
                    "entertainment", "gaming", "concert", "concert-alt-1", "concert-alt-2",
                    "performance"
            )),
            Map.entry("education", List.of(
                    "educational-material", "tuition", "stationery", "stationery-alt-1",
                    "stationery-alt-2"
            )),
            Map.entry("social", List.of(
                    "gift", "flowers", "charity", "communication"
            )),
            Map.entry("income", List.of(
                    "salary", "bonus", "part-time", "business", "investment-income",
                    "reimbursement", "other-income", "credit-card", "insurance", "top-up",
                    "transfer", "transaction-details", "statistics", "bookkeeping"
            )),
            Map.entry("other", List.of(
                    "other-expense", "edit", "settings"
            ))
    );

    private BookkeepingCategoryCatalog() {
    }

    public static boolean isIconKey(String key) {
        return key != null && ICONS.contains(key);
    }
}
