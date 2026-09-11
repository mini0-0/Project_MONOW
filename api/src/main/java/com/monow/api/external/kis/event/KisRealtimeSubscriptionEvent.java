package com.monow.api.external.kis.event;

import com.monow.api.external.kis.type.CurrentPriceMarketType;

public record KisRealtimeSubscriptionEvent(
        CurrentPriceMarketType marketType,
        String stockCode,
        boolean success,
        String message
) {
}
