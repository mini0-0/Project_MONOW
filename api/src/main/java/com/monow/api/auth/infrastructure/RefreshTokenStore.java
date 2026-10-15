package com.monow.api.auth.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, Duration expiration) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                expiration
        );
    }

    public String get(Long userId) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

    }

    public void delete(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

    }


}
