package com.monow.api.auth.application;

import com.monow.api.auth.application.command.AccessTokenReissueCommand;
import com.monow.api.auth.application.command.LoginCommand;
import com.monow.api.auth.application.command.SignUpCommand;
import com.monow.api.auth.application.result.AccessTokenReissueResult;
import com.monow.api.auth.application.result.LoginResult;
import com.monow.api.auth.application.result.SignUpResult;
import com.monow.api.auth.infrastructure.RefreshTokenStore;
import com.monow.domain.account.entity.Account;
import com.monow.domain.account.entity.AccountStatus;
import com.monow.domain.account.service.AccountNumberGenerator;
import com.monow.domain.user.entity.*;
import com.monow.domain.account.repository.AccountRepository;
import com.monow.domain.user.repository.UserRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import com.monow.global.security.jwt.JwtProperties;
import com.monow.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    private static final Long USER_ID = 1L;
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(1);

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private AuthService authService;



    @Nested
    @DisplayName("회원가입 ")
    class SignUp {

        @Test
        @DisplayName("[성공] - 회원가입 성공")
        void signUp_whenValidRequest_savesUser() {
            // Given
            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);

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
            given(passwordEncoder.encode(command.password()))
                    .willReturn(ENCODED_PASSWORD);
            given(userRepository.save(any(User.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(accountNumberGenerator.generate())
                    .willReturn("MONOW260514123456");

            // When
            SignUpResult result = authService.signUp(command);

            // Then
            verify(userRepository).save(userArgumentCaptor.capture());
            verify(accountRepository).save(accountArgumentCaptor.capture());

            User savedUser = userArgumentCaptor.getValue();
            Account account = accountArgumentCaptor.getValue();

            // User 검증
            assertThat(savedUser.getEmail()).isEqualTo(command.email());
            assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(savedUser.getName()).isEqualTo(command.name());
            assertThat(savedUser.getNickname()).isEqualTo(command.nickname());
            assertThat(savedUser.getUserRole()).isEqualTo(UserRole.USER);
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);

            // AuthResult 검증
            assertThat(result.email()).isEqualTo(command.email());
            assertThat(result.name()).isEqualTo(command.name());
            assertThat(result.nickname()).isEqualTo(command.nickname());
            assertThat(result.userRole()).isEqualTo(UserRole.USER);
            assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);


            // Account 검증
            assertThat(account.getUser()).isEqualTo(savedUser);
            assertThat(account.getAccountNumber()).isEqualTo("MONOW260514123456");
            assertThat(account.getSeedMoney()).isEqualByComparingTo(new BigDecimal("100000000.00"));
            assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100000000.00"));
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);


            verify(userRepository).existsByEmail(command.email());
            verify(userRepository).existsByNickname(command.nickname());
            verify(passwordEncoder).encode(command.password());

        }

        @Test
        @DisplayName("[실패] - 이메일 중복")
        void signUp_whenEmailAlreadyExists_throwsDuplicatedEmail() {
            // Given
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
            verify(passwordEncoder, never()).encode(command.password());

        }

        @Test
        @DisplayName("[실패] - 닉네임 중복")
        void signUp_whenNicknameAlreadyExists_throwsDuplicatedNickname(){
            // Given
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
            verify(passwordEncoder, never()).encode(command.password());

        }

        @Test
        @DisplayName("[실패] - 비밀번호와 비밀번호 확인 불일치")
        void signUp_whenPasswordConfirmMismatch_throwsPasswordConfirmMismatch() {
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
            verify(passwordEncoder, never()).encode(command.password());


        }
    }


    @Nested
    @DisplayName("로그인")
    class login{
        @Test
        @DisplayName("[성공] - 로그인 성공")
        void GivenValidCredentials_WhenLogin_ThenReturnLoginResult() {
            // Given
            LoginCommand command = new LoginCommand(
                    "test@test.com",
                    "1234"
            );

            User user = createUser();

            given(userRepository.findByEmail(command.email()))
                    .willReturn(Optional.of(user));
            given(passwordEncoder.matches(command.password(), user.getPassword()))
                    .willReturn(true);
            given(jwtTokenProvider.createAccessToken(user.getId(), user.getUserRole().name()))
                    .willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(user.getId()))
                    .willReturn(REFRESH_TOKEN);
            given(jwtProperties.refreshTokenExpiration())
                    .willReturn(REFRESH_TOKEN_EXPIRATION);

            // When
            LoginResult result = authService.login(command);

            // Then
            assertThat(result.userId()).isEqualTo(user.getId());
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);

            verify(userRepository).findByEmail(command.email());
            verify(passwordEncoder).matches(command.password(), user.getPassword());
            verify(jwtTokenProvider).createAccessToken(user.getId(), user.getUserRole().name());
            verify(jwtTokenProvider).createRefreshToken(user.getId());
            verify(refreshTokenStore).save(USER_ID, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION);

        }

        @Test
        @DisplayName("[실패] - 회원가입 안한 계정")
        void login_whenEmailNotRegistered_throwsLoginFail() {
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
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).createAccessToken(any(), anyString());
            verify(jwtTokenProvider, never()).createRefreshToken(any());


        }

        @Test
        @DisplayName("[실패] - 비밀번호 불일치")
        void login_whenPasswordInvalid_throwsLoginFail() {
            // Given
            LoginCommand command = new LoginCommand(
                    "test@test.com",
                    "0000"
            );

            User user = User.createUser(
                    "test@test.com",
                    ENCODED_PASSWORD,
                    "홍길동",
                    "워렌버핏"
            );

            given(userRepository.findByEmail(command.email()))
                    .willReturn(Optional.of(user));
            given(passwordEncoder.matches(command.password(), user.getPassword()))
                    .willReturn(false);

            // When
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(command));

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAIL);

            verify(userRepository).findByEmail(command.email());
            verify(passwordEncoder).matches(command.password(), user.getPassword());
            verify(jwtTokenProvider, never()).createAccessToken(any(), anyString());
            verify(jwtTokenProvider, never()).createRefreshToken(any());
            verify(refreshTokenStore, never()).save(anyLong(), anyString(), any(Duration.class));

        }

    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("[성공] - 로그아웃 시 Refresh Token 삭제")
        void GivenAuthenticatedUser_WhenLogout_ThenDeleteRefreshToken() {
            // Given

            // When
            authService.logout(USER_ID);

            // Then
            verify(refreshTokenStore).delete(USER_ID);

        }



    }

    @Nested
    @DisplayName("Access Token 재발급")
    class AccessTokenReissue {

        @Test
        @DisplayName("[성공] - 유효한 Refresh Token으로 Access Token 재발급")
        void GivenValidRefreshToken_WhenReissueAccessToken_ThenReturnAccessToken() {
            // Given
            AccessTokenReissueCommand command = new AccessTokenReissueCommand(REFRESH_TOKEN);
            User user = createUser();

            given(jwtTokenProvider.validateToken(REFRESH_TOKEN))
                    .willReturn(true);
            given(jwtTokenProvider.isRefreshToken(REFRESH_TOKEN))
                    .willReturn(true);
            given(jwtTokenProvider.getUserId(REFRESH_TOKEN))
                    .willReturn(USER_ID);
            given(refreshTokenStore.get(USER_ID))
                    .willReturn(REFRESH_TOKEN);
            given(userRepository.findById(USER_ID))
                    .willReturn(Optional.of(user));
            given(jwtTokenProvider.createAccessToken(USER_ID, UserRole.USER.name()))
                    .willReturn(ACCESS_TOKEN);

            // When
            AccessTokenReissueResult result = authService.reissueAccessToken(command);

            // Then
            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);

            verify(jwtTokenProvider).validateToken(REFRESH_TOKEN);
            verify(jwtTokenProvider).isRefreshToken(REFRESH_TOKEN);
            verify(jwtTokenProvider).getUserId(REFRESH_TOKEN);
            verify(refreshTokenStore).get(USER_ID);
            verify(userRepository).findById(USER_ID);
            verify(jwtTokenProvider).createAccessToken(USER_ID, UserRole.USER.name());


        }

        @Test
        @DisplayName("[실패] - 유효하지 않은 Refresh Token")
        void GivenInvalidRefreshToken_WhenReissueAccessToken_ThenThrowInvalidRefreshToken() {
            // Given
            AccessTokenReissueCommand command = new AccessTokenReissueCommand(REFRESH_TOKEN);

            given(jwtTokenProvider.validateToken(REFRESH_TOKEN))
                    .willReturn(false);

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> authService.reissueAccessToken(command)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

            verify(jwtTokenProvider).validateToken(REFRESH_TOKEN);
            verify(jwtTokenProvider, never()).isRefreshToken(anyString());
            verify(jwtTokenProvider, never()).getUserId(anyString());
            verify(refreshTokenStore, never()).get(anyLong());
            verify(userRepository, never()).findById(anyLong());
            verify(jwtTokenProvider, never()).createAccessToken(anyLong(), anyString());

        }

        @Test
        @DisplayName("[실패] - Redis의 Refresh Token과 불일치")
        void GivenRefreshTokenMismatch_WhenReissueAccessToken_ThenThrowInvalidRefreshToken() {
            // Given
            AccessTokenReissueCommand command = new AccessTokenReissueCommand(REFRESH_TOKEN);

            given(jwtTokenProvider.validateToken(REFRESH_TOKEN))
                    .willReturn(true);
            given(jwtTokenProvider.isRefreshToken(REFRESH_TOKEN))
                    .willReturn(true);
            given(jwtTokenProvider.getUserId(REFRESH_TOKEN))
                    .willReturn(USER_ID);
            given(refreshTokenStore.get(USER_ID))
                    .willReturn(null);

            // When & Then
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> authService.reissueAccessToken(command)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

            verify(refreshTokenStore).get(USER_ID);
            verify(userRepository, never()).findById(anyLong());
            verify(jwtTokenProvider, never()).createAccessToken(anyLong(), anyString());



        }


    }


    private User createUser() {
        User user = User.createUser(
                "test@test.com",
                ENCODED_PASSWORD,
                "홍길동",
                "워렌버핏"
        );

        ReflectionTestUtils.setField(user, "id", USER_ID);

        return user;
    }
}
