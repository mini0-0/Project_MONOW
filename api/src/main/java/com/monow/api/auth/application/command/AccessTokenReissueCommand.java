package com.monow.api.auth.application.command;

public record AccessTokenReissueCommand(
        String refreshToken
) {
}
