package com.monow.api.stock.dto.response;

public record StockCurrentPriceResponse(
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
        String updatedAt

) {
}
