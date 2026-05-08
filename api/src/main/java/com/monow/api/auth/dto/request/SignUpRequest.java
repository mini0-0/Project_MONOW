package com.monow.api.auth.dto.request;


import com.monow.domain.auth.dto.command.SignUpCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank(message = "이메일은 필수 입력값 입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력값 입니다.")
        String password,

        @NotBlank(message = "비밀번호 확인은 필수 입력값 입니다.")
        String confirmPassword,

        @NotBlank(message = "이름은 필수 입력값 입니다.")
        String name,

        @NotBlank(message = "닉네임은 필수 입력값 입니다.")
        String nickname
) {

    public SignUpCommand toCommand() {
        return new SignUpCommand(
                email,
                password,
                confirmPassword,
                name,
                nickname
        );
    }



}
