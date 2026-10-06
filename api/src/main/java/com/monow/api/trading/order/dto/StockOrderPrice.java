package com.monow.api.trading.order.dto;

import com.monow.external.kis.type.CurrentPriceMarketType;

import java.math.BigDecimal;

public record StockOrderPrice(
        CurrentPriceMarketType marketType,
        BigDecimal price
) {
}
