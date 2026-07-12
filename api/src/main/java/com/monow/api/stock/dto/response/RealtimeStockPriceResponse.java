package com.monow.api.stock.dto.response;

import com.monow.api.external.kis.type.CurrentPriceMarketType;

import java.time.LocalDateTime;

public record RealtimeStockPriceResponse(
        CurrentPriceMarketType marketType,
        String stockCode,
        String currentPrice,
        String changePrice,
        String changeSign,
        String changeRate,
        String tradeVolume,
        String tradeAmount,
        String openPrice,
        String highPrice,
        String lowPrice,
        String tradeTime,
        LocalDateTime updatedAt
) {
}
