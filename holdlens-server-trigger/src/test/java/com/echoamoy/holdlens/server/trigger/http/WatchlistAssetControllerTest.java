package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.request.WatchlistAssetBatchAddRequestDTO;
import com.echoamoy.holdlens.server.api.request.WatchlistAssetRemoveRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.api.response.WatchlistAssetBatchAddResponseDTO;
import com.echoamoy.holdlens.server.cases.portfolio.IWatchlistAssetBatchAddCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddCommand;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

public class WatchlistAssetControllerTest {

    @Test
    public void batchAddMapsInvalidItemsOnly() {
        FakeWatchlistAssetBatchAddCase batchAddCase = new FakeWatchlistAssetBatchAddCase();
        WatchlistAssetController controller = new WatchlistAssetController(batchAddCase);

        Response<WatchlistAssetBatchAddResponseDTO> response = controller.batchAdd(WatchlistAssetBatchAddRequestDTO.builder()
                .userId(1001L)
                .items(List.of(WatchlistAssetBatchAddRequestDTO.Item.builder()
                        .assetKind("fund")
                        .assetCode(" ")
                        .market(null)
                        .build()))
                .build());

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals(Long.valueOf(1001L), batchAddCase.command.getUserId());
        Assert.assertEquals("fund", batchAddCase.command.getItems().get(0).getAssetKind());
        Assert.assertEquals(1, response.getData().getInvalidItems().size());
        Assert.assertEquals(Integer.valueOf(0), response.getData().getInvalidItems().get(0).getIndex());
        Assert.assertEquals("ASSET_CODE_REQUIRED", response.getData().getInvalidItems().get(0).getReasonCode());
        long businessFieldCount = java.util.Arrays.stream(WatchlistAssetBatchAddResponseDTO.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .count();
        Assert.assertEquals(1L, businessFieldCount);
    }

    @Test
    public void batchAddUsesWatchlistEndpoint() throws Exception {
        Method method = WatchlistAssetController.class.getMethod("batchAdd", WatchlistAssetBatchAddRequestDTO.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);

        Assert.assertNotNull(mapping);
        Assert.assertArrayEquals(new String[]{"/api/watchlist/assets/batch-add"}, mapping.value());
    }

    @Test
    public void batchAddRequestRejectsMissingUserAndEmptyItems() {
        WatchlistAssetBatchAddRequestDTO request = WatchlistAssetBatchAddRequestDTO.builder()
                .items(List.of())
                .build();

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<WatchlistAssetBatchAddRequestDTO>> violations = validator.validate(request);

            Assert.assertEquals(2, violations.size());
            Assert.assertTrue(violations.stream().anyMatch(violation -> "userId".equals(violation.getPropertyPath().toString())));
            Assert.assertTrue(violations.stream().anyMatch(violation -> "items".equals(violation.getPropertyPath().toString())));
        }
    }

    @Test
    public void removeUsesPostActionEndpointAndJsonBody() throws Exception {
        FakeWatchlistAssetBatchAddCase batchAddCase = new FakeWatchlistAssetBatchAddCase();
        WatchlistAssetController controller = new WatchlistAssetController(batchAddCase);
        WatchlistAssetRemoveRequestDTO request = WatchlistAssetRemoveRequestDTO.builder()
                .userId(1001L)
                .assetKind("stock")
                .assetRef("stock:US_STOCK:DEMO")
                .build();

        controller.remove(request);

        Assert.assertEquals(Long.valueOf(1001L), batchAddCase.removedUserId);
        Assert.assertEquals("stock", batchAddCase.removedAssetKind);
        Assert.assertEquals("stock:US_STOCK:DEMO", batchAddCase.removedAssetRef);
        Method method = WatchlistAssetController.class.getMethod("remove", WatchlistAssetRemoveRequestDTO.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        Assert.assertNotNull(mapping);
        Assert.assertArrayEquals(new String[]{"/api/watchlist/assets/remove"}, mapping.value());
    }

    @Test
    public void removeRequestRejectsMissingFields() {
        WatchlistAssetRemoveRequestDTO request = WatchlistAssetRemoveRequestDTO.builder().build();

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<WatchlistAssetRemoveRequestDTO>> violations =
                    validatorFactory.getValidator().validate(request);

            Assert.assertEquals(3, violations.size());
        }
    }

    private static class FakeWatchlistAssetBatchAddCase implements IWatchlistAssetBatchAddCase {
        private WatchlistAssetBatchAddCommand command;
        private Long removedUserId;
        private String removedAssetKind;
        private String removedAssetRef;

        @Override
        public WatchlistAssetBatchAddResult batchAdd(WatchlistAssetBatchAddCommand command) {
            this.command = command;
            return WatchlistAssetBatchAddResult.builder()
                    .invalidItems(List.of(WatchlistAssetBatchAddResult.InvalidItem.builder()
                            .index(0)
                            .assetKind("fund")
                            .assetCode(" ")
                            .market(null)
                            .reasonCode("ASSET_CODE_REQUIRED")
                            .reason("资产代码不能为空")
                            .build()))
                    .build();
        }

        @Override
        public void remove(Long userId, String assetKind, String assetRef) {
            removedUserId = userId;
            removedAssetKind = assetKind;
            removedAssetRef = assetRef;
        }
    }

}
