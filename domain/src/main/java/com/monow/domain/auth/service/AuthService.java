package com.monow.domain.auth.service;

import com.monow.domain.auth.dto.command.LoginCommand;
import com.monow.domain.auth.dto.command.SignUpCommand;
import com.monow.domain.auth.dto.result.AuthResult;
import com.monow.domain.user.entity.User;
import com.monow.domain.user.repository.UserRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;


    public AuthResult signUp(SignUpCommand command) {
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
                command.password(),
                command.name(),
                command.nickname()
        );

        User savedUser = userRepository.save(user);
        
        return AuthResult.from(savedUser);
    }

    public AuthResult login(LoginCommand command) {
         User user = userRepository.findByEmail(command.email())
                 .orElseThrow(() -> {
                         log.warn("로그인 실패 - 가입되지 않은 이메일: {}", command.email());
                         return new BusinessException(ErrorCode.LOGIN_FAIL);
                 });

         if (!user.getPassword().equals(command.password())) {
             log.warn("로그인 실패 - 비밀번호 불일치: userId={}, email={}",
                     user.getId(),
                     user.getEmail());
             throw new BusinessException(ErrorCode.LOGIN_FAIL);
         }
        return AuthResult.from(user);
    }
}
