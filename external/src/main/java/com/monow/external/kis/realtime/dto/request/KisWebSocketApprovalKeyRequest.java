package com.monow.external.kis.realtime.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisWebSocketApprovalKeyRequest(
        @JsonProperty("grant_type")

        String grantType,

        @JsonProperty("appkey")
        String appKey,

        @JsonProperty("secretkey")
        String appSecret
) {
}
