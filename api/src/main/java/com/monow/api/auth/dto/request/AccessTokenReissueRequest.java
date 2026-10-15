package com.monow.api.auth.dto.request;

import com.monow.api.auth.application.command.AccessTokenReissueCommand;
import jakarta.validation.constraints.NotBlank;

public record AccessTokenReissueRequest(
        @NotBlank(message = "Refresh Token은 필수 입력값입니다.")
        String refreshToken
) {
    public AccessTokenReissueCommand toCommand() {
        return new AccessTokenReissueCommand(refreshToken);
    }

}
