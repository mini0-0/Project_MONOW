package com.monow.api.stock.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockDetailResponse(
        String stockCode,
        String stockName,
        String marketName,
        String industryName,
        BigDecimal currentPrice,
        BigDecimal changePrice,
        String changeSign,
        BigDecimal changeRate,
        Long tradeVolume,
        BigDecimal tradeAmount,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
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