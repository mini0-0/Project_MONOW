package com.monow.api.external.kis.application;

import com.monow.api.external.kis.client.KisTokenClient;
import com.monow.api.external.kis.dto.response.KisTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KisAccessTokenProvider {
    private static final long LOCK_WAIT_MILLISECONDS = 200L;

    private final KisTokenClient kisTokenClient;

    private final KisAccessTokenCacheService kisAccessTokenCacheService;

    private final KisAccessTokenLockService kisAccessTokenLockService;

    public String getAccessToken() {
        String redisCachedToken = kisAccessTokenCacheService.getAccessToken();

        if (redisCachedToken != null && !redisCachedToken.isBlank()) {
            return redisCachedToken;
        }

        String lockValue = kisAccessTokenLockService.tryLock();

        if (lockValue == null) {
            return waitAndGetCachedToken();
        }

        try {
            String cachedTokenAfterLock = kisAccessTokenCacheService.getAccessToken();

            if (isValidToken(cachedTokenAfterLock)) {
                return cachedTokenAfterLock;
            }

            KisTokenResponse tokenResponse = kisTokenClient.issueToken();

            String newAccessToken = tokenResponse.accessToken();

            kisAccessTokenCacheService.save(newAccessToken, tokenResponse.expiresIn());

            return newAccessToken;

            } finally {
                kisAccessTokenLockService.unlock(lockValue);
        }
    }

    private String waitAndGetCachedToken() {
        try {
            Thread.sleep(LOCK_WAIT_MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException("KIS accessToken wait interrupted", e);
        }

        String cachedToken = kisAccessTokenCacheService.getAccessToken();

        if (isValidToken(cachedToken)) {
            return cachedToken;
        }
        throw new RuntimeException("KIS accessToken 발급 중이지만 Redis에서 토큰을 찾을 수 없습니다.");

    }

    private boolean isValidToken(String accessToken) {
        return accessToken != null && !accessToken.isBlank();
    }

}

