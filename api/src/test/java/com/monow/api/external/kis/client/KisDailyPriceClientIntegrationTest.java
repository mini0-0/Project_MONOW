package com.monow.api.external.kis.client;

import com.monow.api.external.kis.dto.response.KisDailyPriceResponse;
import com.monow.api.external.kis.dto.response.KisTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class KisDailyPriceClientIntegrationTest {

    @Autowired
    private KisTokenClient kisTokenClient;

    @Autowired
    private KisDailyPriceClient kisDailyPriceClient;

    @Nested
    @DisplayName("한국투자증권 일별 시세 API 연동")
    class DailyPriceApi{

        @Test
        @DisplayName("[성공] - 삼성전자 일별 시세 조회")
        void fetchDailyPrice_whenValidStockCodeProvided_returnsDailyPrices() {
            // Given
            KisTokenResponse tokenResponse = kisTokenClient.issueToken();
            String accessToken = tokenResponse.accessToken();

            String stockCode = "005930";
            assertThat(accessToken).isNotBlank();

            // When
            KisDailyPriceResponse response = kisDailyPriceClient.fetchDailyPrice(accessToken, stockCode);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.rtCd()).isEqualTo("0");
            assertThat(response.output()).isNotNull();
            assertThat(response.output()).isNotEmpty();

            KisDailyPriceResponse.Output first = response.output().get(0);

            assertThat(first.tradeDate()).isNotBlank();
            assertThat(first.openPrice()).isNotBlank();
            assertThat(first.highPrice()).isNotBlank();
            assertThat(first.lowPrice()).isNotBlank();
            assertThat(first.closePrice()).isNotBlank();
            assertThat(first.volume()).isNotBlank();

            assertThat(first.tradeDate()).hasSize(8);
            assertThat(first.openPrice()).containsOnlyDigits();
            assertThat(first.highPrice()).containsOnlyDigits();
            assertThat(first.lowPrice()).containsOnlyDigits();
            assertThat(first.closePrice()).containsOnlyDigits();
            assertThat(first.volume()).containsOnlyDigits();

            log.info(
                    "daily price: date={}, open={}, high={}, low={}, close={}, volume={}",
                    first.tradeDate(),
                    first.openPrice(),
                    first.highPrice(),
                    first.lowPrice(),
                    first.closePrice(),
                    first.volume()
            );

        }
    }
}
