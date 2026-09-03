package com.monow.api.stock.dto.response;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.currentprice.StockMarketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockCurrentPriceResponse(
        CurrentPriceMarketType marketType,
        StockMarketStatus marketStatus,
        boolean realtime,
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
        LocalDateTime updatedAt
) {
}
