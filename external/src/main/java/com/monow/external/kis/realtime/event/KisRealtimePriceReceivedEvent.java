package com.monow.external.kis.realtime.event;

import com.monow.external.kis.realtime.model.KisRealtimePriceData;

public record KisRealtimePriceReceivedEvent(
        KisRealtimePriceData data
) {
}
