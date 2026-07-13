package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FundRefreshSqlStructureTest {

    private static final List<String> NEW_COLUMNS = List.of(
            "fund_type", "pinyin_abbr", "pinyin_full", "return_coverage_status",
            "unit_nav", "accumulated_nav", "daily_growth_rate", "catalog_fetched_at",
            "purchase_status_fetched_at", "period_return_fetched_at", "top_holding_fetched_at",
            "last_detail_view_time");

    @Test
    public void baselineAndForwardMigrationContainEquivalentFundSliceColumns() throws Exception {
        Path root = projectRoot();
        String baseline = Files.readString(root.resolve("docs/dev-ops/mysql/sql/holdlens.sql"));
        String migration = Files.readString(root.resolve("docs/dev-ops/mysql/sql/migrations/20260712_fund_refresh_slices.sql"));
        for (String column : NEW_COLUMNS) {
            Assert.assertTrue("baseline missing " + column, baseline.contains("`" + column + "`"));
            Assert.assertTrue("migration missing " + column, migration.contains("`" + column + "`"));
        }
        Assert.assertTrue(baseline.contains("CREATE TABLE `fund_top_holding`"));
        Assert.assertTrue(baseline.contains("UNIQUE KEY `uk_fund_top_holding_fund_rank` (`fund_code`, `rank_no`)"));
        Assert.assertFalse(baseline.contains("processing_task_item"));
        Assert.assertFalse(migration.contains("processing_task_item"));
    }

    @Test
    public void mapperUsesSliceSpecificStatements() throws Exception {
        String mapper = Files.readString(projectRoot().resolve("holdlens-server-app/src/main/resources/mybatis/mapper/fund_mapper.xml"));
        Assert.assertTrue(mapper.contains("id=\"upsertCatalog\""));
        Assert.assertTrue(mapper.contains("id=\"updatePurchaseStatus\""));
        Assert.assertTrue(mapper.contains("id=\"updatePeriodReturn\""));
        Assert.assertTrue(mapper.contains("id=\"updateTopHoldingMetadata\""));
        Assert.assertTrue(mapper.contains("id=\"selectTopHoldingRefreshTargets\""));
        Assert.assertFalse(mapper.contains("id=\"upsert\""));
        Assert.assertFalse(mapper.contains("id=\"upsertTarget\""));
        Assert.assertFalse(mapper.contains("id=\"selectRefreshTargetsAfterId\""));
        String taskMapper = Files.readString(projectRoot().resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/processing_task_mapper.xml"));
        Assert.assertTrue(taskMapper.contains("id=\"markCallbackFailedIfTimedOut\""));
        Assert.assertTrue(taskMapper.contains("status IN ('created', 'dispatched', 'running')"));
    }

    private Path projectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("docs/dev-ops/mysql/sql/holdlens.sql"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("cannot locate holdlens-server project root");
        return current;
    }
}
