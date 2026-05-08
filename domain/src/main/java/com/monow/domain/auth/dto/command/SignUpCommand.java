package com.monow.domain.auth.dto.command;


public record SignUpCommand(
        String email,
        String password,
        String confirmPassword,
        String name,
        String nickname
) {
}
