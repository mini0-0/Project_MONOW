package com.monow.api.auth.dto.response;

import com.monow.api.auth.application.result.AccessTokenReissueResult;

public record AccessTokenReissueResponse(
        String accessToken
) {
    public static AccessTokenReissueResponse from(AccessTokenReissueResult result) {
        return new AccessTokenReissueResponse(result.accessToken());
    }

}
