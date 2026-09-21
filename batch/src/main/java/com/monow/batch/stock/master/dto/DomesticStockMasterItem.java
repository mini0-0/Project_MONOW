package com.monow.batch.stock.master.dto;

import com.monow.domain.stock.entity.DomesticStockMarketType;

public record DomesticStockMasterItem(
        String stockCode,
        DomesticStockMarketType marketType,
        boolean krxTradable,
        boolean nxtTradable
) {
}
