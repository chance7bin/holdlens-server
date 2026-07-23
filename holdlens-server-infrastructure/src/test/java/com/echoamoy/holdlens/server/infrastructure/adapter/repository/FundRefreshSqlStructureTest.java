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
        int batchStart = mapper.indexOf("<insert id=\"upsertCatalogBatch\"");
        int batchEnd = mapper.indexOf("</insert>", batchStart);
        Assert.assertTrue(batchStart >= 0);
        Assert.assertTrue(batchEnd > batchStart);
        String batchSql = mapper.substring(batchStart, batchEnd);
        Assert.assertTrue(batchSql.contains("collection=\"funds\""));
        Assert.assertTrue(batchSql.contains("item=\"fund\""));
        Assert.assertTrue(batchSql.contains("separator=\",\""));
        Assert.assertTrue(batchSql.contains("#{fund.fundCode}"));
        Assert.assertTrue(batchSql.contains("fund_name = VALUES(fund_name)"));
        Assert.assertTrue(batchSql.contains("fund_type = VALUES(fund_type)"));
        Assert.assertTrue(batchSql.contains("pinyin_abbr = VALUES(pinyin_abbr)"));
        Assert.assertTrue(batchSql.contains("pinyin_full = VALUES(pinyin_full)"));
        Assert.assertTrue(batchSql.contains("catalog_fetched_at = VALUES(catalog_fetched_at)"));
        Assert.assertTrue(mapper.contains("id=\"updatePurchaseStatus\""));
        Assert.assertTrue(mapper.contains("id=\"updatePeriodReturn\""));
        Assert.assertTrue(mapper.contains("id=\"updateTopHoldingMetadata\""));
        Assert.assertTrue(mapper.contains("id=\"selectTopHoldingRefreshTargets\""));
        Assert.assertTrue(mapper.contains("FROM watchlist_item wi"));
        Assert.assertTrue(mapper.contains("FROM asset_record ar"));
        Assert.assertFalse(mapper.contains("FROM asset_info"));
        Assert.assertFalse(mapper.contains("FROM asset_holding"));
        Assert.assertFalse(mapper.contains("id=\"upsert\""));
        Assert.assertFalse(mapper.contains("id=\"upsertTarget\""));
        Assert.assertFalse(mapper.contains("id=\"selectRefreshTargetsAfterId\""));
        String taskMapper = Files.readString(projectRoot().resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/processing_task_mapper.xml"));
        Assert.assertTrue(taskMapper.contains("id=\"markCallbackFailedIfTimedOut\""));
        Assert.assertTrue(taskMapper.contains("id=\"selectByServerTaskIdForUpdate\""));
        Assert.assertTrue(taskMapper.contains("FOR UPDATE"));
        Assert.assertTrue(taskMapper.contains("status IN ('created', 'dispatched', 'running')"));
        Assert.assertEquals(2, occurrences(taskMapper, "NOT EXISTS ("));
        Assert.assertEquals(2, occurrences(taskMapper, "FROM processing_callback callback_record"));

        String callbackMapper = Files.readString(projectRoot().resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/processing_callback_mapper.xml"));
        Assert.assertTrue(callbackMapper.contains("id=\"selectProcessingCatalogCallbacksCreatedBefore\""));
        Assert.assertTrue(callbackMapper.contains("callback_record.process_status = 'processing'"));
        Assert.assertTrue(callbackMapper.contains("task.task_type = 'fund_catalog_refresh'"));
        Assert.assertTrue(callbackMapper.contains("task.status IN ('created', 'dispatched', 'running')"));
    }

    @Test
    public void assetAllocationBaselineMigrationAndMapperUseIndependentTripleUniqueSnapshot() throws Exception {
        Path root = projectRoot();
        String baseline = Files.readString(root.resolve("docs/dev-ops/mysql/sql/holdlens.sql"));
        String migration = Files.readString(root.resolve(
                "docs/dev-ops/mysql/sql/migrations/20260716_fund_asset_allocation_refresh.sql"));
        for (String column : List.of("asset_allocation_as_of", "asset_allocation_status",
                "asset_allocation_fetched_at")) {
            Assert.assertTrue("baseline missing " + column, baseline.contains("`" + column + "`"));
            Assert.assertTrue("migration missing " + column, migration.contains("`" + column + "`"));
        }
        String tripleUnique = "(`fund_code`, `asset_type`, `asset_type_name`)";
        Assert.assertTrue(baseline.contains("CREATE TABLE `fund_asset_allocation`"));
        Assert.assertTrue(migration.contains("CREATE TABLE `fund_asset_allocation`"));
        Assert.assertTrue(baseline.contains(tripleUnique));
        Assert.assertTrue(migration.contains(tripleUnique));
        Assert.assertTrue(baseline.contains("`allocation_ratio` DECIMAL(12, 4) NOT NULL"));
        Assert.assertTrue(migration.contains("`allocation_ratio` DECIMAL(12, 4) NOT NULL"));
        Assert.assertFalse(migration.contains("DROP TABLE"));
        Assert.assertFalse(migration.contains("fund_top_holding"));

        String mapper = Files.readString(root.resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/fund_asset_allocation_mapper.xml"));
        Assert.assertTrue(mapper.contains("id=\"insertBatch\""));
        Assert.assertTrue(mapper.contains("id=\"deleteByFundCode\""));
        Assert.assertTrue(mapper.contains("id=\"selectByFundCodes\""));
        Assert.assertTrue(mapper.contains("display_order ASC, asset_type ASC, asset_type_name ASC"));

        String fundMapper = Files.readString(root.resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/fund_mapper.xml"));
        Assert.assertTrue(fundMapper.contains("id=\"selectAssetAllocationMetadataForUpdate\""));
        Assert.assertTrue(fundMapper.contains("asset_allocation_as_of &lt;= #{assetAllocationAsOf}"));
        Assert.assertTrue(fundMapper.contains("f.asset_allocation_status = 'missing'"));
        Assert.assertTrue(fundMapper.contains("f.asset_allocation_status = 'unavailable'"));
        Assert.assertTrue(fundMapper.contains("f.asset_allocation_fetched_at &lt;= #{unavailableRetryBefore}"));

        String taskMapper = Files.readString(root.resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/processing_task_mapper.xml"));
        Assert.assertEquals(2, occurrences(taskMapper, "'fund_asset_allocation_refresh'"));
    }

    private int occurrences(String value, String target) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
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
