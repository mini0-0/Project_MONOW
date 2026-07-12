package com.monow.api.stock.dto.response;

import java.time.LocalDateTime;

public record StockDetailResponse(
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
        LocalDateTime updatedAt,
        String webSocketEndpoint,
        String realtimeTopic
) {
    public static StockDetailResponse from(
            StockCurrentPriceResponse currentPrice,
            String webSocketEndpoint,
            String realtimeTopic
    ) {
        return new StockDetailResponse(
                currentPrice.stockCode(),
                currentPrice.stockName(),
                currentPrice.marketName(),
                currentPrice.industryName(),
                currentPrice.currentPrice(),
                currentPrice.changePrice(),
                currentPrice.changeSign(),
                currentPrice.changeRate(),
                currentPrice.tradeVolume(),
                currentPrice.tradeAmount(),
                currentPrice.openPrice(),
                currentPrice.highPrice(),
                currentPrice.lowPrice(),
                currentPrice.updatedAt(),
                webSocketEndpoint,
                realtimeTopic
        );
    }
}