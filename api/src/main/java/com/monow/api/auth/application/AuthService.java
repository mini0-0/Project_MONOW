package com.monow.api.auth.application;

import com.monow.api.auth.application.command.AccessTokenReissueCommand;
import com.monow.api.auth.application.command.LoginCommand;
import com.monow.api.auth.application.command.SignUpCommand;
import com.monow.api.auth.application.result.AccessTokenReissueResult;
import com.monow.api.auth.application.result.LoginResult;
import com.monow.api.auth.infrastructure.RefreshTokenStore;
import com.monow.domain.account.service.AccountNumberGenerator;
import com.monow.api.auth.application.result.SignUpResult;
import com.monow.domain.account.entity.Account;
import com.monow.domain.user.entity.User;
import com.monow.domain.account.repository.AccountRepository;
import com.monow.domain.user.repository.UserRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import com.monow.global.security.jwt.JwtProperties;
import com.monow.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService  {

    private static final BigDecimal DEFAULT_SEED_MONEY = new BigDecimal("100000000.00");

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProperties jwtProperties;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public SignUpResult signUp(SignUpCommand command) {
        if (!command.password().equals(command.confirmPassword())){
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        if (userRepository.existsByEmail(command.email())) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);

        }

        if (userRepository.existsByNickname(command.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATED_NICKNAME);
        }

        User user = User.createUser(
                command.email(),
                passwordEncoder.encode(command.password()),
                command.name(),
                command.nickname()
        );

        User savedUser = userRepository.save(user);

        String accountNumber = accountNumberGenerator.generate();

        Account account = Account.createAccount(
                savedUser,
                accountNumber,
                DEFAULT_SEED_MONEY
        );

        accountRepository.save(account);
        
        return SignUpResult.from(savedUser);
    }

    public LoginResult login(LoginCommand command) {
         User user = userRepository.findByEmail(command.email())
                 .orElseThrow(() -> {
                         log.warn("로그인 실패 - 가입되지 않은 이메일: {}", command.email());
                         return new BusinessException(ErrorCode.LOGIN_FAIL);
                 });

         if (!passwordEncoder.matches(command.password(), user.getPassword())) {
             log.warn("로그인 실패 - 비밀번호 불일치: userId={}, email={}",
                     user.getId(),
                     user.getEmail());
             throw new BusinessException(ErrorCode.LOGIN_FAIL);
         }

         String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUserRole().name());
         String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

         refreshTokenStore.save(user.getId(), refreshToken, jwtProperties.refreshTokenExpiration());

        return new LoginResult(
                user.getId(),
                accessToken,
                refreshToken
        );
    }

    public void logout(Long userId) {

        refreshTokenStore.delete(userId);

    }

    public AccessTokenReissueResult reissueAccessToken(AccessTokenReissueCommand command) {

        String refreshToken = command.refreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);

        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String savedRefreshToken = refreshTokenStore.get(userId);

        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);

        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUserRole().name());

        return new AccessTokenReissueResult(accessToken);

    }


}
