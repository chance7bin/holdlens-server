package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.request.AssetRequestDTO;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

public class AssetApiContractMappingTest {

    @Test
    public void overviewRecordListAndDetailMappingsMatchContract() throws Exception {
        Method overview = AssetController.class.getMethod("overview", Long.class, String.class);
        assertGet(overview, "/api/assets/overview");
        assertRequestParam(overview, 0, "userId", true);
        assertRequestParam(overview, 1, "targetCurrency", false);

        Method records = AssetController.class.getMethod("queryRecords", Long.class, String.class);
        assertGet(records, "/api/asset-records");
        assertRequestParam(records, 0, "userId", true);
        assertRequestParam(records, 1, "assetRef", false);

        Method detail = AssetController.class.getMethod("queryRecord", Long.class, Long.class);
        assertGet(detail, "/api/asset-records/{recordId}");
        Assert.assertEquals("recordId", detail.getParameters()[0].getAnnotation(PathVariable.class).value());
        assertRequestParam(detail, 1, "userId", true);
    }

    @Test
    public void assetWriteMappingsUsePostActionEndpoints() throws Exception {
        assertPost(AssetController.class.getMethod("updateCatalog", Long.class,
                AssetRequestDTO.UpdateCatalog.class), "/api/asset-catalogs/{catalogId}/update-details");
        assertPost(AssetController.class.getMethod("deleteCatalog", Long.class,
                AssetRequestDTO.UserOperation.class), "/api/asset-catalogs/{catalogId}/delete");
        assertPost(AssetController.class.getMethod("updateRecordDetails", Long.class,
                AssetRequestDTO.UpdateDetails.class), "/api/asset-records/{recordId}/update-details");
        assertPost(AssetController.class.getMethod("updateRecordAmount", Long.class,
                AssetRequestDTO.UpdateAmount.class), "/api/asset-records/{recordId}/update-amount");
        assertPost(AssetController.class.getMethod("upsertExchangeRate",
                AssetRequestDTO.UpsertExchangeRate.class), "/internal/exchange-rates/upsert");
    }

    private void assertGet(Method method, String path) {
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        Assert.assertNotNull(mapping);
        Assert.assertArrayEquals(new String[]{path}, mapping.value());
    }

    private void assertPost(Method method, String path) {
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        Assert.assertNotNull(mapping);
        Assert.assertArrayEquals(new String[]{path}, mapping.value());
    }

    private void assertRequestParam(Method method, int index, String name, boolean required) {
        RequestParam annotation = method.getParameters()[index].getAnnotation(RequestParam.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(name, annotation.value());
        Assert.assertEquals(required, annotation.required());
    }
}
