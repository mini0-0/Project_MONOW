package com.monow.batch.stock.dailyprice.application;

import com.monow.batch.BatchApplication;
import com.monow.external.kis.stock.client.KisDailyPriceClient;
import com.monow.external.kis.stock.dto.response.KisDailyPriceResponse;
import com.monow.external.kis.exception.DailyStockPriceRetryableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = BatchApplication.class)
class DailyStockPriceRetryServiceTest {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String STOCK_CODE = "005930";

    @Autowired
    private DailyStockPriceRetryService dailyStockPriceRetryService;

    @MockitoBean
    private KisDailyPriceClient kisDailyPriceClient;

    @Test
    @DisplayName("[성공] - 일별 시세 API 일시 오류 발생 시 재시도 후 응답 반환")
    void fetchDailyPrice_whenTemporaryErrorOccurs_retriesAndReturnsResponse() {
        // Given
        KisDailyPriceResponse kisDailyPriceResponse = createKisDailyPriceResponse();

        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE))
                .willThrow(new DailyStockPriceRetryableException("일시 오류"))
                .willThrow(new DailyStockPriceRetryableException("일시 오류"))
                .willReturn(kisDailyPriceResponse);

        // When
        KisDailyPriceResponse result = dailyStockPriceRetryService.fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE);

        // Then
        assertThat(result).isEqualTo(kisDailyPriceResponse);

        verify(kisDailyPriceClient, times(3)).fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE);


    }


    @Test
    @DisplayName("[실패] - 일별 시세 API 재시도 횟수 초과 시 예외 발생")
    void fetchDailyPrice_whenRetryLimitExceeded_throwsException() {
        // Given
        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE))
                .willThrow(new DailyStockPriceRetryableException("일시 오류"))
                .willThrow(new DailyStockPriceRetryableException("일시 오류"))
                .willThrow(new DailyStockPriceRetryableException("일시 오류"));

        // When & Then
        assertThatThrownBy(() -> dailyStockPriceRetryService.fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE))
                .isInstanceOf(DailyStockPriceRetryableException.class);

        verify(kisDailyPriceClient, times(3)).fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE);


    }

    private KisDailyPriceResponse createKisDailyPriceResponse() {
        KisDailyPriceResponse.Output output = new KisDailyPriceResponse.Output(
                "20260910",
                "72000",
                "73500",
                "71000",
                "72800",
                "12345678"
        );

        return new KisDailyPriceResponse(
                "0",
                "MCA00000",
                "정상처리 되었습니다.",
                List.of(output)
        );
    }

}