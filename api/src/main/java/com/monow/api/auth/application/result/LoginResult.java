package com.monow.api.auth.application.result;

public record LoginResult(
        Long userId,
        String accessToken,
        String refreshToken
) {
}
