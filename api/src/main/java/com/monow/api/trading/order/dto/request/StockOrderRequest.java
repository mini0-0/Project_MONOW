package com.monow.api.trading.order.dto.request;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.domain.order.entity.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockOrderRequest(
        @NotNull Long userId,
        @NotBlank String stockCode,
        @NotNull CurrentPriceMarketType marketType,
        @NotNull OrderType orderType,
        @Positive int quantity
) {
}
