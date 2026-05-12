package com.monow.domain.auth.service;

import com.monow.domain.auth.dto.command.LoginCommand;
import com.monow.domain.auth.dto.command.SignUpCommand;
import com.monow.domain.auth.dto.result.AuthResult;
import com.monow.domain.user.entity.User;
import com.monow.domain.user.entity.UserRole;
import com.monow.domain.user.entity.UserStatus;
import com.monow.domain.user.repository.UserRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("성공")
        void signUpSuccess() {
            // Given
            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            SignUpCommand command = new SignUpCommand(
                    "test@test.com",
                    "1234",
                    "1234",
                    "홍길동",
                    "워렌버핏"
            );

            given(userRepository.existsByEmail(command.email()))
                    .willReturn(false);
            given(userRepository.existsByNickname(command.nickname()))
                    .willReturn(false);

            given(userRepository.save(any(User.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthResult result = authService.signUp(command);

            // Then
            verify(userRepository).save(userArgumentCaptor.capture());

            User savedUser = userArgumentCaptor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo(command.email());
            assertThat(savedUser.getPassword()).isEqualTo(command.password());
            assertThat(savedUser.getName()).isEqualTo(command.name());
            assertThat(savedUser.getNickname()).isEqualTo(command.nickname());
            assertThat(savedUser.getUserRole()).isEqualTo(UserRole.USER);
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);

            assertThat(result.email()).isEqualTo(command.email());
            assertThat(result.name()).isEqualTo(command.name());
            assertThat(result.nickname()).isEqualTo(command.nickname());
            assertThat(result.userRole()).isEqualTo(UserRole.USER);
            assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);

            verify(userRepository).existsByEmail(command.email());
            verify(userRepository).existsByNickname(command.nickname());


        }

        @Test
        @DisplayName("실패 - 이메일 중복")
        void signUpFail_duplicateEmail() {
            // Given
            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            SignUpCommand command = new SignUpCommand(
                    "test@test.com",
                    "1234",
                    "1234",
                    "홍길동",
                    "워렌버핏"
            );

            given(userRepository.existsByEmail(command.email()))
                    .willReturn(true);

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> authService.signUp(command)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATED_EMAIL);

            verify(userRepository).existsByEmail(command.email());
            verify(userRepository, never()).existsByNickname(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("실패 - 닉네임 중복")
        void signUpFail_duplicateNickname() {
            // Given
            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            SignUpCommand command = new SignUpCommand(
                    "test@test.com",
                    "1234",
                    "1234",
                    "홍길동",
                    "워렌버핏"
            );

            given(userRepository.existsByEmail(command.email()))
                    .willReturn(false);
            given(userRepository.existsByNickname(command.nickname()))
                    .willReturn(true);


            // When & Then

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> authService.signUp(command)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATED_NICKNAME);

            verify(userRepository).existsByEmail(command.email());
            verify(userRepository).existsByNickname(command.nickname());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("실패 - 비밀번호와 비밀번호 확인 불일치")
        void signUpFail_passwordConfirmMisMatch() {
            // Given
            SignUpCommand command = new SignUpCommand(
                    "test@test.com",
                    "1234",
                    "0000",
                    "홍길동",
                    "워렌버핏"
            );

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> authService.signUp(command)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH);

            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).existsByNickname(anyString());
            verify(userRepository, never()).save(any(User.class));


        }

        @Test
        @DisplayName("로그인 - 로그인 성공")
        void login_success() {
            // Given
            LoginCommand command = new LoginCommand(
                    "test@test.com",
                    "1234"
            );

            User user = User.createUser(
                    "test@test.com",
                    "1234",
                    "홍길동",
                    "워렌버핏"
                    );

            given(userRepository.findByEmail(command.email())).willReturn(Optional.of(user));

            // When
            AuthResult result = authService.login(command);

            // Then
            assertThat(result.email()).isEqualTo(command.email());
            assertThat(result.name()).isEqualTo(user.getName());
            assertThat(result.nickname()).isEqualTo(user.getNickname());
            assertThat(result.userRole()).isEqualTo(user.getUserRole());
            assertThat(result.status()).isEqualTo(user.getUserStatus());

            verify(userRepository).findByEmail(command.email());

        }

        @Test
        @DisplayName("로그인 실패 - 회원가입 안한 계정")
        void loginFail_notSignUp() {
            // Given
            LoginCommand command = new LoginCommand(
                    "test@test.com",
                    "1234"
            );

            given(userRepository.findByEmail(command.email()))
                    .willReturn(Optional.empty());

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(command));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAIL);

            verify(userRepository).findByEmail(command.email());

        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호 불일치")
        void loginFail_invalidPassword() {
            // Given
            LoginCommand command = new LoginCommand(
                    "test@test.com",
                    "0000"
            );

            User user = User.createUser(
                    "test@test.com",
                    "1234",
                    "홍길동",
                    "워렌버핏"
            );

            given(userRepository.findByEmail(command.email())).willReturn(Optional.of(user));

            // When
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(command));

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAIL);

            verify(userRepository).findByEmail(command.email());


        }
    }

}
