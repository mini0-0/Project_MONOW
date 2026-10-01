package com.monow.batch.stock.master.application;

import com.monow.batch.BatchApplication;
import com.monow.external.kis.exception.DomesticStockMasterRetryableException;
import com.monow.external.kis.stock.client.KisStockInfoClient;
import com.monow.external.kis.stock.dto.response.KisStockInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = BatchApplication.class)
class DomesticStockMasterRetryServiceTest {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String STOCK_CODE = "005930";

    @Autowired
    private DomesticStockMasterRetryService domesticStockMasterRetryService;

    @MockitoBean
    private KisStockInfoClient kisStockInfoClient;

    @Test
    @DisplayName("[성공] - KIS 종목 정보 조회 실패 후 재시도하여 정상 응답 반환")
    void fetchStockInfo_whenTemporaryFailureOccurs_retriesAndReturnsResponse() {
        // Given
        KisStockInfoResponse kisStockInfoResponse = createKisStockInfoResponse();

        given(kisStockInfoClient.fetchStockInfo(ACCESS_TOKEN, STOCK_CODE))
                .willThrow(new DomesticStockMasterRetryableException("일시 오류"))
                .willThrow(new DomesticStockMasterRetryableException("일시 오류"))
                .willReturn(kisStockInfoResponse);

        // When
        KisStockInfoResponse result = domesticStockMasterRetryService.fetchDomesticStockMaster(ACCESS_TOKEN, STOCK_CODE);

        // Then
        assertThat(result).isEqualTo(kisStockInfoResponse);

        verify(kisStockInfoClient, times(3)).fetchStockInfo(ACCESS_TOKEN, STOCK_CODE);


    }

    @Test
    @DisplayName("[실패] - KIS 종목 정보 조회 최대 재시도 횟수 초과")
    void fetchStockInfo_whenRetryLimitExceeded_throwsRetryableException() {
        // Given
        given(kisStockInfoClient.fetchStockInfo(ACCESS_TOKEN, STOCK_CODE))
                .willThrow(new DomesticStockMasterRetryableException("일시 오류"))
                .willThrow(new DomesticStockMasterRetryableException("일시 오류"))
                .willThrow(new DomesticStockMasterRetryableException("일시 오류"));

        // When & Then
        assertThatThrownBy(() -> domesticStockMasterRetryService.fetchDomesticStockMaster(ACCESS_TOKEN, STOCK_CODE))
                .isInstanceOf(DomesticStockMasterRetryableException.class);

        verify(kisStockInfoClient, times(3)).fetchStockInfo(ACCESS_TOKEN, STOCK_CODE);
    }


    private KisStockInfoResponse createKisStockInfoResponse() {
        KisStockInfoResponse.Output output = new KisStockInfoResponse.Output(
                "00000A005930",
                "KR7005930003",
                "005930",
                "삼성전자보통주",
                "삼성전자",
                "300",
                "101010",
                "주권",
                "1010",
                "주식"
        );

        return new KisStockInfoResponse(
                        "0",
                        "MCA00000",
                        "정상처리 되었습니다.",
                        output
                );

    }


}