package com.monow.api.external.kis.type;

import lombok.Getter;

@Getter
public enum CurrentPriceMarketType {
    KRX("J"),
    NXT("NX"),
    INTEGRATED("UN");

    private final String kisCode;

    CurrentPriceMarketType(String kisCode) {
        this.kisCode = kisCode;
    }

}
