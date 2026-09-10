package com.monow.api.stock.currentprice.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;

public record StockCurrentPriceMarketSelection(
        CurrentPriceMarketType marketType,
        StockMarketStatus marketStatus,
        boolean realtime
) {
}
