package com.monow.api.external.kis.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KisAccessTokenLockService {
    private static final String KIS_ACCESS_TOKEN_LOCK_KEY = "kis:access_token:lock";

    private static final long LOCK_TTL_SECONDS = 5L;

    private final StringRedisTemplate stringRedisTemplate;

    public String tryLock() {
        String lockValue = UUID.randomUUID().toString();

        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(
                        KIS_ACCESS_TOKEN_LOCK_KEY,
                        lockValue,
                        Duration.ofSeconds(LOCK_TTL_SECONDS)
                );

        if (Boolean.TRUE.equals(locked)) {
            return lockValue;
        }


        return null;
    }

    public void unlock(String lockValue) {
        if (lockValue == null || lockValue.isBlank()) {
            return;
        }

        String currentValue = stringRedisTemplate.opsForValue()
                .get(KIS_ACCESS_TOKEN_LOCK_KEY);

        if (lockValue.equals(currentValue)) {
            stringRedisTemplate.delete(KIS_ACCESS_TOKEN_LOCK_KEY);
        }

    }

}
