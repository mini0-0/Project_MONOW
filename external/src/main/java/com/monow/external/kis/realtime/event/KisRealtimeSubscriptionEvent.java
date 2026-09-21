package com.monow.external.kis.realtime.event;


import com.monow.external.kis.type.CurrentPriceMarketType;

public record KisRealtimeSubscriptionEvent(
        CurrentPriceMarketType marketType,
        String stockCode,
        boolean success,
        String message
) {
}
