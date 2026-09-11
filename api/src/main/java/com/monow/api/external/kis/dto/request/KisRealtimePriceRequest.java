package com.monow.api.external.kis.dto.request;

public record KisRealtimePriceRequest(
        String marketCode,
        String stockCode
) {
}
