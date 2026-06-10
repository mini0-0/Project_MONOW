package com.monow.api.external.kis.dto.response;

import com.monow.api.external.kis.stockmaster.DomesticStockMarketType;

public record DomesticStockMasterItem(
        String stockCode,
        String stockName,
        DomesticStockMarketType marketType
) {
}
