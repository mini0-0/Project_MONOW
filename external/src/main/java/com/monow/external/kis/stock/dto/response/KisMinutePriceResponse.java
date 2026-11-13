package com.monow.external.kis.stock.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KisMinutePriceResponse(
        @JsonProperty("rt_cd") String rtCd,
        @JsonProperty("msg_cd") String msgCd,
        @JsonProperty("msg1") String message,
        @JsonProperty("output2") List<Output> output
) {
    public record Output(
            // 주식 영업 일자
            @JsonProperty("stck_bsop_date") String tradeDate,
            // 주식 체결 시간
            @JsonProperty("stck_cntg_hour") String tradeTime,
            @JsonProperty("stck_oprc") String openPrice,
            @JsonProperty("stck_hgpr") String highPrice,
            @JsonProperty("stck_lwpr") String lowPrice,
            @JsonProperty("stck_prpr") String closePrice,
            @JsonProperty("cntg_vol") String volume
    ) {
    }
}
