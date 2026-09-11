package com.monow.api.auth.application.command;


public record SignUpCommand(
        String email,
        String password,
        String confirmPassword,
        String name,
        String nickname
) {
}
