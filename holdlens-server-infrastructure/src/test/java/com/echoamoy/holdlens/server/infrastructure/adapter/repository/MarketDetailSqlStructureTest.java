package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MarketDetailSqlStructureTest {

    @Test
    public void baselineAndMigrationContainTheSameDetailTablesAndKeys() throws Exception {
        Path root = projectRoot();
        String baseline = Files.readString(root.resolve("docs/dev-ops/mysql/sql/holdlens.sql"));
        String migration = Files.readString(root.resolve("docs/dev-ops/mysql/sql/migrations/20260717_market_detail_data.sql"));
        for (String table : List.of("fund_nav_history", "stock_price_bar", "stock_company_profile")) {
            Assert.assertTrue(baseline.contains("CREATE TABLE `" + table + "`"));
            Assert.assertTrue(migration.contains("CREATE TABLE `" + table + "`"));
        }
        Assert.assertTrue(migration.contains("(`fund_code`,`nav_date`)"));
        Assert.assertTrue(migration.contains("(`stock_code`,`market`,`granularity`,`bar_time`)"));
        Assert.assertTrue(migration.contains("(`stock_code`,`market`)"));
        Assert.assertFalse(migration.contains("DROP TABLE"));
    }

    @Test
    public void mapperUsesParameterizedBatchUpsertAndAscendingQueries() throws Exception {
        String mapper = Files.readString(projectRoot().resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/market_detail_mapper.xml"));
        Assert.assertTrue(mapper.contains("id=\"upsertFundNavHistory\""));
        Assert.assertTrue(mapper.contains("collection=\"points\""));
        Assert.assertTrue(mapper.contains("id=\"upsertStockPriceBars\""));
        Assert.assertTrue(mapper.contains("collection=\"bars\""));
        Assert.assertTrue(mapper.contains("ON DUPLICATE KEY UPDATE"));
        Assert.assertTrue(mapper.contains("ORDER BY nav_date ASC"));
        Assert.assertTrue(mapper.contains("ORDER BY bar_time ASC"));
        Assert.assertFalse(mapper.contains("${"));
    }

    private Path projectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("docs/dev-ops/mysql/sql/holdlens.sql"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("cannot locate project root");
        return current;
    }
}
