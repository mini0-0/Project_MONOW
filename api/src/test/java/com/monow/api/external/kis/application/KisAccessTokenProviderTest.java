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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KisAccessTokenProviderTest {

    @Mock
    private KisTokenClient kisTokenClient;

    private KisAccessTokenProvider kisAccessTokenProvider;

    private MutableClock clock;

    @BeforeEach
    void setUp() {
        this.clock = new MutableClock(
                Instant.parse("2026-05-30T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );

        this.kisAccessTokenProvider = new KisAccessTokenProvider(
                kisTokenClient,
                clock
        );
    }

    @Nested
    @DisplayName("KIS AccessToken 캐싱 기능")
    class KisAccessTokenCaching {

        @Test
        @DisplayName("[성공] - 저장된 토큰이 없으면 신규 토큰 발급을 요청")
        void getAccessToken_whenCachedTokenDoesNotExist_callsIssueToken() {
            // Given
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
            verify(kisTokenClient, times(1)).issueToken();
        }

        @Test
        @DisplayName("[성공] - 저장된 토큰이 유효하면 기존 토큰을 재사용")
        void getAccessToken_whenCachedTokenIsValid_reusesCachedToken() {
            // Given
            KisTokenResponse tokenResponse = new KisTokenResponse(
                    "accessToken",
                    "Bearer",
                    86400L
            );

            given(kisTokenClient.issueToken())
                    .willReturn(tokenResponse);

            // When
            String firstToken = kisAccessTokenProvider.getAccessToken();
            String secondToken = kisAccessTokenProvider.getAccessToken();

            // Then
            assertThat(firstToken).isEqualTo(secondToken);
            assertThat(secondToken).isEqualTo("accessToken");
            verify(kisTokenClient, times(1)).issueToken();
        }

        @Test
        @DisplayName("[성공] - 저장된 토큰이 만료되었으면 신규 토큰 발급을 요청")
        void getAccessToken_whenCachedTokenExpired_callsIssueToken() {
            // Given
            KisTokenResponse firstResponse = new KisTokenResponse(
                    "accessToken1",
                    "Bearer",
                    10L
            );

            KisTokenResponse secondResponse = new KisTokenResponse(
                    "accessToken2",
                    "Bearer",
                    86400L
            );

            given(kisTokenClient.issueToken())
                    .willReturn(firstResponse)
                    .willReturn(secondResponse);

            // When
            String firstToken = kisAccessTokenProvider.getAccessToken();

            clock.plusSeconds(11);

            String secondToken = kisAccessTokenProvider.getAccessToken();

            // Then
            assertThat(firstToken).isEqualTo("accessToken1");
            assertThat(secondToken).isEqualTo("accessToken2");
            verify(kisTokenClient, times(2)).issueToken();
        }

        @Test
        @DisplayName("[성공] - 저장된 토큰의 만료가 임박하면 신규 토큰 발급을 요청")
        void getAccessToken_whenCachedTokenExpiresSoon_callsIssueToken() {
            // Given
            KisTokenResponse firstResponse = new KisTokenResponse(
                    "accessToken1",
                    "Bearer",
                    120L
            );

            KisTokenResponse secondResponse = new KisTokenResponse(
                    "accessToken2",
                    "Bearer",
                    86400L
            );

            given(kisTokenClient.issueToken())
                    .willReturn(firstResponse)
                    .willReturn(secondResponse);

            // When
            String firstToken = kisAccessTokenProvider.getAccessToken();

            clock.plusSeconds(70);

            String secondToken = kisAccessTokenProvider.getAccessToken();

            // Then
            assertThat(firstToken).isEqualTo("accessToken1");
            assertThat(secondToken).isEqualTo("accessToken2");
            verify(kisTokenClient, times(2)).issueToken();
        }
    }

    static class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void plusSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return this.zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(this.instant, zone);
        }

        @Override
        public Instant instant() {
            return this.instant;
        }
    }
}