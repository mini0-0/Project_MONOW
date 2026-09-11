package com.monow.api.stock.search.dto.response;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.repository.StockSearchQueryResult;

public record StockSearchResponse(
        String stockCode,
        String stockName,
        DomesticStockMarketType marketType
) {
    public static StockSearchResponse from(StockSearchQueryResult result) {

        return new StockSearchResponse(
                result.stockCode(),
                result.stockName(),
                result.marketType()
        );

    }
}
