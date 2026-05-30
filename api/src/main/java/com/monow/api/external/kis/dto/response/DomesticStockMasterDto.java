package com.monow.api.external.kis.dto.response;

import com.monow.api.external.kis.stockmaster.DomesticStockMarketType;

public record DomesticStockMasterDto(
        String stockCode,
        String stockName,
        DomesticStockMarketType marketType
) {
}
