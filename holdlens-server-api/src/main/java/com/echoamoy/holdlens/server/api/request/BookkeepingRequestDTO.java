package com.echoamoy.holdlens.server.api.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class BookkeepingRequestDTO {

    private BookkeepingRequestDTO() {
    }

    @Data
    public static class CreateCategoryDTO {

        @NotNull
        @Positive
        private Long userId;

        @NotBlank
        @Size(max = 64)
        private String requestId;

        @NotBlank
        private String type;

        @NotBlank
        @Size(max = 16)
        private String name;

        @NotBlank
        @Size(max = 64)
        private String iconKey;
    }

    @Data
    public static class CategoryOperationDTO {

        @NotNull
        @Positive
        private Long userId;
    }

    @Data
    public static class ReorderCategoriesDTO {

        @NotNull
        @Positive
        private Long userId;

        @NotBlank
        private String type;

        @NotNull
        @Size(min = 1)
        private List<@NotBlank @Size(max = 50) String> categoryCodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateEntryDTO {

        @NotNull
        @Positive
        private Long userId;

        @NotBlank
        @Size(max = 64)
        private String requestId;

        @NotBlank
        private String type;

        @NotBlank
        private String categoryCode;

        @NotNull
        @Positive
        @Digits(integer = 18, fraction = 2)
        private BigDecimal amount;

        @NotNull
        private LocalDate entryDate;

        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviseEntryDTO {

        @NotNull
        @Positive
        private Long userId;

        @NotBlank
        private String type;

        @NotBlank
        private String categoryCode;

        @NotNull
        @Positive
        @Digits(integer = 18, fraction = 2)
        private BigDecimal amount;

        @NotNull
        private LocalDate entryDate;

        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserOperationDTO {

        @NotNull
        @Positive
        private Long userId;
    }
}
