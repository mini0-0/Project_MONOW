package com.monow.api.auth.controller;


import com.monow.domain.auth.dto.command.SignUpCommand;
import com.monow.domain.auth.dto.result.AuthResult;
import com.monow.domain.auth.service.AuthService;
import com.monow.domain.user.entity.UserRole;
import com.monow.domain.user.entity.UserStatus;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.handler.GlobalExceptionHandler;
import com.monow.global.error.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
public class AuthControllerTest {
    @Autowired private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("회원가입 API 성공")
    void signupApiSuccess() throws Exception {
        // Given
        String requestBody = """
        {
            "email": "test@test.com",
            "password": "1234",
            "confirmPassword": "1234",
            "name": "홍길동",
            "nickname": "워렌버핏"
        }
        """;

        AuthResult authResult = new AuthResult(
                1L,
                "test@test.com",
                "1234",
                "홍길동",
                "워렌버핏",
                UserRole.USER,
                UserStatus.ACTIVE);

        given(authService.signUp(any(SignUpCommand.class)))
                .willReturn(authResult);


        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
//                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.nickname").value("워렌버핏"))
                .andExpect(jsonPath("$.data.userRole").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(authService).signUp(any(SignUpCommand.class));
    }

    @Test
    @DisplayName("회원가입 API 실패 - 비밀번호 확인 불일치")
    void signupApiFail_PasswordBlank() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "test@test.com",
                    "password": "1234",
                    "confirmPassword": "0000",
                    "name" : "홍길동",
                    "nickname" : "워렌버핏"
                
                }
                """;

        given(authService.signUp(any(SignUpCommand.class)))
                .willThrow(new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PASSWORD_CONFIRM_MISMATCH"))
                .andExpect(jsonPath("$.error.message").value("비밀번호가 일치하지 않습니다."));
        ;

    }


    @Test
    @DisplayName("회원가입 API 실패 - 이미 존재하는 이메일")
    void signupApiFail_DuplicateEmail() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "test@test.com",
                    "password": "1234",
                    "confirmPassword" : "1234",
                    "name" : "홍길동",
                    "nickname" : "워렌버핏"
                
                }
                """;

        given(authService.signUp(any(SignUpCommand.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATED_EMAIL));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATED_EMAIL"))
                .andExpect(jsonPath("$.error.message").value("이미 가입된 이메일입니다."));

    }

    @Test
    @DisplayName("회원가입 API 실패 - 이미 존재하는 닉네임")
    void signupApiFail_DuplicateNickname() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "test@test.com",
                    "password": "1234",
                    "confirmPassword" : "1234",
                    "name" : "홍길동",
                    "nickname" : "워렌버핏"
                
                }
                """;

        given(authService.signUp(any(SignUpCommand.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATED_NICKNAME));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATED_NICKNAME"))
                .andExpect(jsonPath("$.error.message").value("이미 사용 중인 닉네임입니다."));

    }

    @Test
    @DisplayName("회원가입 API 실패 - 이메일 형식이 오류")
    void signupApiFail_InvalidEmail() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "test-email",
                    "password": "1234",
                    "confirmPassword" : "1234",
                    "name" : "홍길동",
                    "nickname" : "워렌버핏"
                
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("올바른 이메일 형식이 아닙니다."));

    }


    @Test
    @DisplayName("회원가입 API 실패 - 이메일 누락")
    void signupApiFail_EmailBlank() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "",
                    "password": "1234",
                    "confirmPassword" : "1234",
                    "name" : "홍길동",
                    "nickname" : "워렌버핏"
                
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("이메일은 필수 입력값 입니다."));

    }

    @Test
    @DisplayName("회원가입 API 실패 - 이름 누락")
    void signupApiFail_NameBlank() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "test@test.com",
                    "password": "1234",
                    "confirmPassword" : "1234",
                    "name" : "",
                    "nickname" : "워렌버핏"
                
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("이름은 필수 입력값 입니다."));

    }

    @Test
    @DisplayName("회원가입 API 실패 - 닉네임 누락")
    void signupApiFail_NicknameBlank() throws Exception {
        // Given
        String requestBody = """
                {
                    "email": "test@test.com",
                    "password": "1234",
                    "confirmPassword" : "1234",
                    "name" : "홍길동",
                    "nickname" : ""
                
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("닉네임은 필수 입력값 입니다."));

    }






    // 로그인 api 성공

    // 로그인 api 실패

}
