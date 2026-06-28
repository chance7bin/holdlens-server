package com.echoamoy.holdlens.server.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuoteRefreshCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Valid
    @NotEmpty
    private List<Stock> stocks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stock implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotBlank
        private String stockCode;

        @NotBlank
        private String market;

    }

}
