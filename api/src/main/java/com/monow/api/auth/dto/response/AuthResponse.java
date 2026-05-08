package com.monow.api.auth.dto.response;

import com.monow.domain.auth.dto.result.AuthResult;
import com.monow.domain.user.entity.UserRole;
import com.monow.domain.user.entity.UserStatus;

public record AuthResponse(
        Long userId,
        String email,
        String name,
        String nickname,
        UserRole userRole,
        UserStatus status
) {
        public static AuthResponse from(AuthResult result) {
            return new AuthResponse(
                    result.userId(),
                    result.email(),
                    result.name(),
                    result.nickname(),
                    result.userRole(),
                    result.status());

        }


}
