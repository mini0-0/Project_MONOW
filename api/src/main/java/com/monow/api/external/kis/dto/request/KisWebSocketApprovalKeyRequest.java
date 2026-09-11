package com.monow.api.external.kis.dto.request;

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
