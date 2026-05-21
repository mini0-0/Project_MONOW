package com.monow.api.external.kis.client;

import com.monow.api.external.kis.dto.KisTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class KisTokenClientIntegrationTest {

    @Autowired
    private KisTokenClient kisTokenClient;


    @Nested
    @DisplayName("한국투자증권 API 연동")
    class KisApi {

        @Test
        @DisplayName("access token 발급")
        void kisAccessToken() {
            // Given

            // When
            KisTokenResponse response = kisTokenClient.issueToken();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.tokenType()).isNotBlank();
            assertThat(response.expiresIn()).isNotNull();


        }

    }
}
