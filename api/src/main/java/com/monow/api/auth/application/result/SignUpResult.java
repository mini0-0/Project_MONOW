package com.monow.api.auth.application.result;

import com.monow.domain.user.entity.User;
import com.monow.domain.user.entity.UserRole;
import com.monow.domain.user.entity.UserStatus;

public record SignUpResult(
        Long userId,
        String email,
        String name,
        String nickname,
        UserRole userRole,
        UserStatus status
) {

    public static SignUpResult from(User user) {
        return new SignUpResult(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getUserRole(),
                user.getUserStatus());
    }
}
