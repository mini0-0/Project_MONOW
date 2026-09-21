package com.monow.external.kis.realtime.dto.request;

public record KisRealtimePriceRequest(
        String marketCode,
        String stockCode
) {
}
