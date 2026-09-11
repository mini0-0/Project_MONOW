package com.monow.api.external.kis.dto.response;

import com.monow.domain.stock.entity.DomesticStockMarketType;

public record DomesticStockMasterItem(
        String stockCode,
        String stockName,
        DomesticStockMarketType marketType,
        boolean krxTradable,
        boolean nxtTradable
) {
}
