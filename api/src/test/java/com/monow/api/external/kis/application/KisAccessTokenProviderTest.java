package com.monow.api.external.kis.application;

import com.monow.api.external.kis.client.KisTokenClient;

import com.monow.api.external.kis.dto.response.KisTokenResponse;
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
            given(kisAccessTokenCacheService.getAccessToken())
                    .willReturn(null, null);

            given(kisAccessTokenLockService.tryLock())
                    .willReturn("lockValue");

            KisTokenResponse tokenResponse = new KisTokenResponse(
                    "accessToken",
                    "Bearer",
                    86400L
            );

            given(kisTokenClient.issueToken())
                    .willReturn(tokenResponse);

            // When
            String accessToken = kisAccessTokenProvider.getAccessToken();

            // Then
            assertThat(accessToken).isEqualTo("accessToken");

            verify(kisAccessTokenLockService, times(1)).tryLock();
            verify(kisTokenClient, times(1)).issueToken();
            verify(kisAccessTokenCacheService, times(1))
                    .save(
                            eq("accessToken"),
                            eq(86400L)
                    );
            verify(kisAccessTokenLockService, times(1))
                    .unlock("lockValue");

        }

        @Test
        @DisplayName("[성공] - Redis 저장된 토큰이 유효하면 기존 토큰을 재사용")
        void getAccessToken_whenCachedTokenIsValid_reusesCachedToken() {
            // Given
            given(kisAccessTokenCacheService.getAccessToken())
                    .willReturn("cachedAccessToken");

            // When
            String accessToken = kisAccessTokenProvider.getAccessToken();

            // Then
            assertThat(accessToken).isEqualTo("cachedAccessToken");

            verify(kisTokenClient, never()).issueToken();
            verify(kisAccessTokenCacheService, never())
                    .save(
                            anyString(),
                            anyLong()
                    );
            verify(kisAccessTokenLockService, never()).tryLock();
            verify(kisAccessTokenLockService, never()).unlock(anyString());
        }

        @Test
        @DisplayName("[성공] - lock 획득에 실패했지만 대기 후 Redis 토큰이 있으면 기존 토큰을 반환")
        void getAccessToken_whenLockFailsButCachedTokenExistsAfterWait_returnsCachedToken() {
            // Given
            given(kisAccessTokenCacheService.getAccessToken())
                    .willReturn(null, "cachedAccessTokenAfterWait");

            given(kisAccessTokenLockService.tryLock())
                    .willReturn(null);

            // When
            String accessToken = kisAccessTokenProvider.getAccessToken();

            // Then
            assertThat(accessToken).isEqualTo("cachedAccessTokenAfterWait");

            verify(kisAccessTokenLockService, times(1)).tryLock();
            verify(kisTokenClient, never()).issueToken();
            verify(kisAccessTokenCacheService, never())
                    .save(
                            anyString(),
                            anyLong()
                    );
            verify(kisAccessTokenLockService, never()).unlock(anyString());
        }

        @Test
        @DisplayName("[실패] - Redis에 토큰이 없고 KIS 토큰 발급에 실패하면 예외가 발생")
        void getAccessToken_whenIssueTokenFails_throwsException() {
            // Given
            given(kisAccessTokenCacheService.getAccessToken())
                    .willReturn(null, null);

            given(kisAccessTokenLockService.tryLock())
                    .willReturn("lockValue");

            given(kisTokenClient.issueToken())
                    .willThrow(new RuntimeException("KIS token issue failed"));


            // When & Then
            assertThatThrownBy(() -> kisAccessTokenProvider.getAccessToken())
                    .isInstanceOf(RuntimeException.class);

            verify(kisAccessTokenLockService, times(1)).tryLock();
            verify(kisTokenClient, times(1)).issueToken();
            verify(kisAccessTokenCacheService, never())
                    .save(
                            anyString(),
                            anyLong()
                    );
            verify(kisAccessTokenLockService, times(1))
                    .unlock("lockValue");

        }

    }

}