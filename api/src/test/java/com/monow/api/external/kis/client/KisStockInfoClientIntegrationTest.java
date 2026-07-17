package com.monow.api.external.kis.client;

import com.monow.api.external.kis.application.KisAccessTokenProvider;
import com.monow.api.external.kis.dto.response.KisStockInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class KisStockInfoClientIntegrationTest {

    @Autowired
    private KisAccessTokenProvider kisAccessTokenProvider;

    @Autowired
    private KisStockInfoClient kisStockInfoClient;

    @Nested
    @DisplayName("한국투자증권 종목 정보 API 연동")
    class StockApi {

        @Test
        @DisplayName("[성공] - 삼성전자 종목 정보 조회")
        void fetchStockInfo_whenValidStockCodeProvided_returnsStockInfo() {

            // Given
            String accessToken = kisAccessTokenProvider.getAccessToken();

            String stockCode = "005930";

            assertThat(accessToken).isNotBlank();

            // When
            KisStockInfoResponse response = kisStockInfoClient.fetchStockInfo(accessToken, stockCode);


            // Then
            assertThat(response).isNotNull();
            assertThat(response.rtCd()).isEqualTo("0");
            assertThat(response.output()).isNotNull();

            KisStockInfoResponse.Output output = response.output();

            assertThat(output.shortProductNumber()).isEqualTo("005930");
            assertThat(output.productShortName()).isNotBlank();
            assertThat(output.productName()).isNotBlank();
            assertThat(output.productTypeCode()).isEqualTo("300");
            assertThat(output.investmentProductTypeName()).isEqualTo("주식");
            assertThat(output.productClassName()).isEqualTo("주권");

            log.info("stock: output = {}", output );

        }

    }
}
