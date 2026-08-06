package com.monow.api.stock.dto.request;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StockSyncRequest(
        @NotEmpty(message = "종목코드는 1개 이상 입력해야 합니다.")
        List<@Valid StockSyncItem> stocks
) {
        public record StockSyncItem(
                @NotBlank(message = "종목 코드는 필수입니다.")
                String stockCode,

                @NotNull(message = "시장 구분은 필수입니다.")
                DomesticStockMarketType marketType

        ) {
        }
}
