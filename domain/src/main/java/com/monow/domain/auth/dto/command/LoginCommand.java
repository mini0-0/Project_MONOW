package com.monow.domain.auth.dto.command;

public record LoginCommand(
        String email,
        String password
) {
}
