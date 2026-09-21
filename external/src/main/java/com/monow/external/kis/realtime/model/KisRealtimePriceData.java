package com.monow.external.kis.realtime.model;

import com.monow.external.kis.type.CurrentPriceMarketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record KisRealtimePriceData(
        CurrentPriceMarketType marketType,
        String stockCode,
        BigDecimal currentPrice,
        BigDecimal changePrice,
        String changeSign,
        BigDecimal changeRate,
        Long tradeVolume,
        BigDecimal tradeAmount,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        LocalTime tradeTime,
        LocalDateTime receivedAt
) {
}
