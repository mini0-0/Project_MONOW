package com.monow.external.auth.application;

import com.monow.global.external.kis.client.KisTokenClient;

import com.monow.global.external.kis.dto.response.KisTokenResponse;
import com.monow.global.external.kis.application.KisAccessTokenCacheService;
import com.monow.global.external.kis.application.KisAccessTokenLockService;
import com.monow.global.external.kis.application.KisAccessTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KisAccessTokenProviderTest {

    @Mock
    private KisTokenClient kisTokenClient;

    @Mock
    private KisAccessTokenCacheService kisAccessTokenCacheService;

    @Mock
    private KisAccessTokenLockService kisAccessTokenLockService;

    private KisAccessTokenProvider kisAccessTokenProvider;


    @BeforeEach
    void setUp() {
        this.kisAccessTokenProvider = new KisAccessTokenProvider(
                kisTokenClient,
                kisAccessTokenCacheService,
                kisAccessTokenLockService
        );
    }

    @Nested
    @DisplayName("KIS AccessToken 캐싱 기능")
    class KisAccessTokenCaching {

        @Test
        @DisplayName("[성공] - Redis 저장된 토큰이 없으면 신규 토큰 발급을 요청")
        void getAccessToken_whenCachedTokenDoesNotExist_callsIssueToken() {
            // Given
            BDDMockito.given(kisAccessTokenCacheService.getAccessToken())
                    .willReturn(null, null);

            BDDMockito.given(kisAccessTokenLockService.tryLock())
                    .willReturn("lockValue");

            KisTokenResponse tokenResponse = new KisTokenResponse(
                    "accessToken",
                    "Bearer",
                    86400L
            );

            BDDMockito.given(kisTokenClient.issueToken())
                    .willReturn(tokenResponse);

            // When
            String accessToken = kisAccessTokenProvider.getAccessToken();

            // Then
            Assertions.assertThat(accessToken).isEqualTo("accessToken");

            Mockito.verify(kisAccessTokenLockService, Mockito.times(1)).tryLock();
            Mockito.verify(kisTokenClient, Mockito.times(1)).issueToken();
            Mockito.verify(kisAccessTokenCacheService, Mockito.times(1))
                    .save(
                            ArgumentMatchers.eq("accessToken"),
                            ArgumentMatchers.eq(86400L)
                    );
            Mockito.verify(kisAccessTokenLockService, Mockito.times(1))
                    .unlock("lockValue");

        }

        @Test
        @DisplayName("[성공] - Redis 저장된 토큰이 유효하면 기존 토큰을 재사용")
        void getAccessToken_whenCachedTokenIsValid_reusesCachedToken() {
            // Given
            BDDMockito.given(kisAccessTokenCacheService.getAccessToken())
                    .willReturn("cachedAccessToken");

            // When
            String accessToken = kisAccessTokenProvider.getAccessToken();

            // Then
            Assertions.assertThat(accessToken).isEqualTo("cachedAccessToken");

            Mockito.verify(kisTokenClient, Mockito.never()).issueToken();
            Mockito.verify(kisAccessTokenCacheService, Mockito.never())
                    .save(
                            ArgumentMatchers.anyString(),
                            ArgumentMatchers.anyLong()
                    );
            Mockito.verify(kisAccessTokenLockService, Mockito.never()).tryLock();
            Mockito.verify(kisAccessTokenLockService, Mockito.never()).unlock(ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("[성공] - lock 획득에 실패했지만 대기 후 Redis 토큰이 있으면 기존 토큰을 반환")
        void getAccessToken_whenLockFailsButCachedTokenExistsAfterWait_returnsCachedToken() {
            // Given
            BDDMockito.given(kisAccessTokenCacheService.getAccessToken())
                    .willReturn(null, "cachedAccessTokenAfterWait");

            BDDMockito.given(kisAccessTokenLockService.tryLock())
                    .willReturn(null);

            // When
            String accessToken = kisAccessTokenProvider.getAccessToken();

            // Then
            Assertions.assertThat(accessToken).isEqualTo("cachedAccessTokenAfterWait");

            Mockito.verify(kisAccessTokenLockService, Mockito.times(1)).tryLock();
            Mockito.verify(kisTokenClient, Mockito.never()).issueToken();
            Mockito.verify(kisAccessTokenCacheService, Mockito.never())
                    .save(
                            ArgumentMatchers.anyString(),
                            ArgumentMatchers.anyLong()
                    );
            Mockito.verify(kisAccessTokenLockService, Mockito.never()).unlock(ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("[실패] - Redis에 토큰이 없고 KIS 토큰 발급에 실패하면 예외가 발생")
        void getAccessToken_whenIssueTokenFails_throwsException() {
            // Given
            BDDMockito.given(kisAccessTokenCacheService.getAccessToken())
                    .willReturn(null, null);

            BDDMockito.given(kisAccessTokenLockService.tryLock())
                    .willReturn("lockValue");

            BDDMockito.given(kisTokenClient.issueToken())
                    .willThrow(new RuntimeException("KIS token issue failed"));


            // When & Then
            Assertions.assertThatThrownBy(() -> kisAccessTokenProvider.getAccessToken())
                    .isInstanceOf(RuntimeException.class);

            Mockito.verify(kisAccessTokenLockService, Mockito.times(1)).tryLock();
            Mockito.verify(kisTokenClient, Mockito.times(1)).issueToken();
            Mockito.verify(kisAccessTokenCacheService, Mockito.never())
                    .save(
                            ArgumentMatchers.anyString(),
                            ArgumentMatchers.anyLong()
                    );
            Mockito.verify(kisAccessTokenLockService, Mockito.times(1))
                    .unlock("lockValue");

        }

    }

}