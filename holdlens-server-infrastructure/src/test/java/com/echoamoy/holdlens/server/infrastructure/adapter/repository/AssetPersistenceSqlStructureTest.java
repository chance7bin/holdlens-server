package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class AssetPersistenceSqlStructureTest {

    @Test
    public void assetMappersUseUserIsolationLockingAndAppendOnlyHistory() throws Exception {
        Path mapperRoot = projectRoot().resolve("holdlens-server-app/src/main/resources/mybatis/mapper");
        String catalog = Files.readString(mapperRoot.resolve("asset_catalog_mapper.xml"));
        String record = Files.readString(mapperRoot.resolve("asset_record_mapper.xml"));
        String history = Files.readString(mapperRoot.resolve("asset_record_change_mapper.xml"));
        String watchlist = Files.readString(mapperRoot.resolve("watchlist_item_mapper.xml"));
        String rates = Files.readString(mapperRoot.resolve("exchange_rate_mapper.xml"));

        Assert.assertTrue(catalog.contains("user_id = #{userId}"));
        Assert.assertTrue(record.contains("ar.user_id = #{userId}"));
        Assert.assertTrue(record.contains("FOR UPDATE"));
        Assert.assertTrue(history.contains("<insert id=\"insertBatch\">"));
        Assert.assertFalse(history.contains("<update"));
        Assert.assertFalse(history.contains("<delete"));
        Assert.assertTrue(watchlist.contains("user_id = #{userId}"));
        Assert.assertTrue(watchlist.contains("asset_id = #{assetId}"));
        Assert.assertTrue(rates.contains("ON DUPLICATE KEY UPDATE"));
        Assert.assertFalse(catalog.contains("${"));
        Assert.assertFalse(record.contains("${"));
        Assert.assertFalse(history.contains("${"));
        Assert.assertFalse(watchlist.contains("${"));
        Assert.assertFalse(rates.contains("${"));
    }

    @Test
    public void baselineCreatesNewAssetModelAndInvestmentHierarchy() throws Exception {
        String ddl = Files.readString(projectRoot().resolve("docs/dev-ops/mysql/sql/holdlens.sql"));

        Assert.assertTrue(ddl.contains("CREATE TABLE `asset_catalog`"));
        Assert.assertTrue(ddl.contains("CREATE TABLE `asset_record`"));
        Assert.assertTrue(ddl.contains("CREATE TABLE `asset_record_change`"));
        Assert.assertTrue(ddl.contains("CREATE TABLE `watchlist_item`"));
        Assert.assertTrue(ddl.contains("CREATE TABLE `exchange_rate`"));
        Assert.assertTrue(ddl.contains("'INVESTMENT_ASSET', '投资资产'"));
        Assert.assertTrue(ddl.contains("'FUND', '基金'"));
        Assert.assertTrue(ddl.contains("'STOCK', '股票'"));
        Assert.assertFalse(ddl.contains("CREATE TABLE `asset_account`"));
        Assert.assertFalse(ddl.contains("CREATE TABLE `asset_info`"));
        Assert.assertFalse(ddl.contains("CREATE TABLE `asset_holding`"));
    }

    private Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        if (Files.exists(current.resolve("holdlens-server-app"))) return current;
        return current.getParent();
    }
}
