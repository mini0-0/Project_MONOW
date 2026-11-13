package com.monow.domain.stock.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

public record StockPriceMinute(
        long stockId,
        String marketCode,
        OffsetDateTime barAt,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        long volume,
        OffsetDateTime collectedAt
) {
    public StockPriceMinute {
        Objects.requireNonNull(barAt);
        Objects.requireNonNull(collectedAt);
        Objects.requireNonNull(openPrice);
        Objects.requireNonNull(highPrice);
        Objects.requireNonNull(lowPrice);
        Objects.requireNonNull(closePrice);

        if (stockId <= 0 || !"UN".equals(marketCode) || volume < 0
                || barAt.getSecond() != 0 || barAt.getNano() != 0
                || lowPrice.signum() < 0 || highPrice.compareTo(lowPrice) < 0
                || openPrice.compareTo(lowPrice) < 0 || openPrice.compareTo(highPrice) > 0
                || closePrice.compareTo(lowPrice) < 0 || closePrice.compareTo(highPrice) > 0) {
            throw new IllegalArgumentException("분봉 식별정보, 가격 또는 거래량이 잘못되었습니다.");
        }

        for (BigDecimal price : new BigDecimal[]{openPrice, highPrice, lowPrice, closePrice}) {
            if (price.stripTrailingZeros().scale() > 2 || price.abs().compareTo(new BigDecimal("10000000000000000")) >= 0) {
                throw new IllegalArgumentException("가격이 NUMERIC(18, 2) 범위를 벗어났습니다.");
            }
        }
    }
}
