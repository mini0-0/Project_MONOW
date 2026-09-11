package com.monow.api.external.kis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisWebSocketApprovalKeyResponse(
        @JsonProperty("approval_key")
        String approvalKey,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        Long expiresIn

) {
}
