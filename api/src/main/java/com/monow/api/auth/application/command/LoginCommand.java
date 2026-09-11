package com.monow.api.auth.application.command;

public record LoginCommand(
        String email,
        String password
) {
}
