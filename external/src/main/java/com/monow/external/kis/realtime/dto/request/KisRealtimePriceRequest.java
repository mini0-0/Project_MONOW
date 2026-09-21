package com.monow.external.kis.dto.request;

public record KisRealtimePriceRequest(
        String marketCode,
        String stockCode
) {
}
