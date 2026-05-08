package com.monow.domain.auth.dto.result;

import com.monow.domain.user.entity.User;
import com.monow.domain.user.entity.UserRole;
import com.monow.domain.user.entity.UserStatus;

public record AuthResult(
        Long userId,
        String email,
        String password,
        String name,
        String nickname,
        UserRole userRole,
        UserStatus status
) {

    public static AuthResult from(User user) {
        return new AuthResult(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getName(),
                user.getNickname(),
                user.getUserRole(),
                user.getUserStatus());
    }
}
