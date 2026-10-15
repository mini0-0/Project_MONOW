package com.monow.api.auth.dto.response;

import com.monow.api.auth.application.result.SignUpResult;
import com.monow.domain.user.entity.UserRole;
import com.monow.domain.user.entity.UserStatus;

public record SignUpResponse(
        Long userId,
        String email,
        String name,
        String nickname,
        UserRole userRole,
        UserStatus status
) {
        public static SignUpResponse from(SignUpResult result) {
            return new SignUpResponse(
                    result.userId(),
                    result.email(),
                    result.name(),
                    result.nickname(),
                    result.userRole(),
                    result.status());

        }


}
