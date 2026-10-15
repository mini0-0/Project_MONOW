package com.monow.api.auth.controller;

import com.monow.api.auth.application.AuthService;
import com.monow.api.auth.application.command.AccessTokenReissueCommand;
import com.monow.api.auth.application.result.AccessTokenReissueResult;
import com.monow.api.auth.application.result.LoginResult;
import com.monow.api.auth.dto.request.AccessTokenReissueRequest;
import com.monow.api.auth.dto.request.LoginRequest;
import com.monow.api.auth.dto.request.SignUpRequest;
import com.monow.api.auth.dto.response.AccessTokenReissueResponse;
import com.monow.api.auth.dto.response.LoginResponse;
import com.monow.api.auth.dto.response.SignUpResponse;
import com.monow.api.auth.application.command.LoginCommand;
import com.monow.api.auth.application.command.SignUpCommand;
import com.monow.api.auth.application.result.SignUpResult;
import com.monow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpCommand command = request.toCommand();
        SignUpResult result = authService.signUp(command);
        SignUpResponse response = SignUpResponse.from(result);

        return ApiResponse.success(response);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = request.toCommand();
        LoginResult result = authService.login(command);
        LoginResponse response = LoginResponse.from(result);

        return ApiResponse.success(response);

    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        Long userId = (Long) principal;

        authService.logout(userId);

        return ApiResponse.success(null);
    }


    @PostMapping("/reissue")
    public ApiResponse<AccessTokenReissueResponse> reissue(@Valid @RequestBody AccessTokenReissueRequest request) {
        AccessTokenReissueCommand command = request.toCommand();
        AccessTokenReissueResult result = authService.reissueAccessToken(command);
        AccessTokenReissueResponse response = AccessTokenReissueResponse.from(result);

        return ApiResponse.success(response);

    }


}
