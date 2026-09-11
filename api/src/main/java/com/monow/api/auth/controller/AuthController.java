package com.monow.api.auth.controller;

import com.monow.api.auth.application.AuthService;
import com.monow.api.auth.dto.request.LoginRequest;
import com.monow.api.auth.dto.request.SignUpRequest;
import com.monow.api.auth.dto.response.AuthResponse;
import com.monow.api.auth.application.command.LoginCommand;
import com.monow.api.auth.application.command.SignUpCommand;
import com.monow.api.auth.application.result.AuthResult;
import com.monow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpCommand command = request.toCommand();
        AuthResult result = authService.signUp(command);
        AuthResponse response = AuthResponse.from(result);
        return ApiResponse.success(response);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = request.toCommand();
        AuthResult result = authService.login(command);
        AuthResponse response = AuthResponse.from(result);
        return ApiResponse.success(response);

    }
}
