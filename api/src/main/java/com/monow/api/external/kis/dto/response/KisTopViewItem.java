package com.monow.api.external.kis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisTopViewItem(
        @JsonProperty("mrkt_div_cls_code")
        String marketCode,
        @JsonProperty("mksc_shrn_iscd")
        String stockCode
) {
}
