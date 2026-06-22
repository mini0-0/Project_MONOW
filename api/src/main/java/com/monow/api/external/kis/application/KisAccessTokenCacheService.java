package com.monow.api.external.kis.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class KisAccessTokenCacheService {

    private static final String KIS_ACCESS_TOKEN_KEY = "kis:access_token";

    private static final long TOKEN_EXPIRE_MARGIN_SECONDS = 60L;

    private final StringRedisTemplate stringRedisTemplate;

    public String getAccessToken() {

        return stringRedisTemplate.opsForValue().get(KIS_ACCESS_TOKEN_KEY);
    }


    public void save(String newAccessToken, Long expiresIn) {
        long redisSeconds = expiresIn - TOKEN_EXPIRE_MARGIN_SECONDS;

        redisSeconds = Math.max(1, redisSeconds);

        stringRedisTemplate.opsForValue()
                .set(KIS_ACCESS_TOKEN_KEY,
                        newAccessToken,
                        Duration.ofSeconds(redisSeconds)
                );

    }
}

