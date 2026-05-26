package com.monow.api.external.kis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.result.Output;

import java.util.List;

public record KisDailyPriceResponse(
        @JsonProperty("rt_cd")
        String rtCd,

        @JsonProperty("msg_cd")
        String msgCd,

        @JsonProperty("msg1")
        String message,

        @JsonProperty("output")
        List<Output> output
) {
    public record Output(
            @JsonProperty("stck_bsop_date")
            String tradeDate,

            @JsonProperty("stck_oprc")
            String openPrice,

            @JsonProperty("stck_hgpr")
            String highPrice,

            @JsonProperty("stck_lwpr")
            String lowPrice,

            @JsonProperty("stck_clpr")
            String closePrice,

            @JsonProperty("acml_vol")
            String volume
    ) {

    }

}
