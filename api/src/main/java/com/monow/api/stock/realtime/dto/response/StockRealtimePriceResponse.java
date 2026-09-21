package com.monow.api.stock.realtime.dto.response;

import com.monow.external.kis.realtime.model.KisRealtimePriceData;
import com.monow.external.kis.type.CurrentPriceMarketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record StockRealtimePriceResponse(
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
        LocalDateTime updatedAt
) {
    public static StockRealtimePriceResponse from(KisRealtimePriceData data) {
        return new StockRealtimePriceResponse(
                data.marketType(),
                data.stockCode(),
                data.currentPrice(),
                data.changePrice(),
                data.changeSign(),
                data.changeRate(),
                data.tradeVolume(),
                data.tradeAmount(),
                data.openPrice(),
                data.highPrice(),
                data.lowPrice(),
                data.tradeTime(),
                data.receivedAt()
        );
    }

}
