package com.monow.external.kis.stock.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record KisMinutePriceRequest(String stockCode, LocalDate tradeDate, LocalTime inputTime) {
    public KisMinutePriceRequest {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("종목코드는 필수입니다.");
        }
        Objects.requireNonNull(tradeDate, "거래일은 필수입니다.");
        Objects.requireNonNull(inputTime, "조회 시각은 필수입니다.");
    }
}
