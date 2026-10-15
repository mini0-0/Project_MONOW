package com.monow.api.auth.dto.response;

import com.monow.api.auth.application.result.LoginResult;

public record LoginResponse(
        Long userId,
        String accessToken,
        String refreshToken
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.userId(),
                result.accessToken(),
                result.refreshToken()
        );

    }
}
