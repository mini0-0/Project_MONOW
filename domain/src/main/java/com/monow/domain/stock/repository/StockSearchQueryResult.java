package com.monow.domain.stock.repository;

import com.monow.domain.stock.entity.DomesticStockMarketType;

public record StockSearchQueryResult(
        String stockCode,
        String stockName,
        String productClassName,
        DomesticStockMarketType marketType,
        Boolean isActive
) {
}
