package com.monow.api.external.kis.type;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum CurrentPriceMarketType {
    KRX("J", "H0STCNT0"),
    NXT("NX", "H0NXCNT0"),
    INTEGRATED("UN", "H0UNCNT0");

    private final String kisCode;

    private final String realtimeTrId;


    CurrentPriceMarketType(String kisCode, String realtimeTrId) {
        this.kisCode = kisCode;
        this.realtimeTrId = realtimeTrId;
    }

    // KIS 시장 코드(J, NX, UN)를 CurrentPriceMarketType으로 변환
    public static CurrentPriceMarketType fromKisCode(String kisCode) {
        return Arrays.stream(values())
                .filter(marketType -> marketType.kisCode.equals(kisCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 KIS 시장 코드입니다. kisCode=" + kisCode));
    }

    // WebSocket TR ID를 CurrentPriceMarketType으로 변환
    public static CurrentPriceMarketType fromRealtimeTrId(String realtimeTrId) {
        return Arrays.stream(values())
                .filter(marketType -> marketType.realtimeTrId.equals(realtimeTrId))
                .findFirst()
                .orElseThrow(() ->  new IllegalArgumentException("지원하지 않는 실시간 현재가 TR ID입니다. realtimeTrId=" + realtimeTrId));
    }

    public static boolean supportsRealtimeTrId(String realtimeTrId) {
        return Arrays.stream(values())
                .anyMatch(marketType ->
                        marketType.realtimeTrId.equals(realtimeTrId)
                );
    }

}
