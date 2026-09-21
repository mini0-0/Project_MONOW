package com.monow.external.kis.stock.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KisTopViewResponse(
        @JsonProperty("rt_cd")
        String rtCd,

        @JsonProperty("msg_cd")
        String msgCd,

        @JsonProperty("msg1")
        String message,

        @JsonProperty("output1")
        List<KisTopViewItem> output
) {
}