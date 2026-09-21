package com.monow.external.kis.stock.dto.response;

import com.monow.domain.stock.entity.DomesticStockMarketType;

public record DomesticStockMasterItem(
        String stockCode,
        DomesticStockMarketType marketType,
        boolean krxTradable,
        boolean nxtTradable
) {
}
