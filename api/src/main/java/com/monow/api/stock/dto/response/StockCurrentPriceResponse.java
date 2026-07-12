package com.monow.api.stock.dto.response;

import com.monow.api.external.kis.type.CurrentPriceMarketType;

import java.time.LocalDateTime;

public record StockCurrentPriceResponse(
        CurrentPriceMarketType marketType,
        String stockCode,
        String stockName,
        String marketName,
        String industryName,
        String currentPrice,
        String changePrice,
        String changeSign,
        String changeRate,
        String tradeVolume,
        String tradeAmount,
        String openPrice,
        String highPrice,
        String lowPrice,
        LocalDateTime updatedAt
) {
}
