package com.monow.domain.holding.repository;

import com.monow.domain.stock.entity.DomesticStockMarketType;

import java.math.BigDecimal;

public record PortfolioHoldingQueryResult(
        String stockCode,
        String stockName,
        DomesticStockMarketType marketType,
        Integer quantity,
        BigDecimal totalPurchaseAmount
) {
}
