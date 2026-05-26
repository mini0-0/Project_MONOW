package com.monow.api.external.kis.application;

import com.monow.api.external.kis.client.KisStockInfoClient;
import com.monow.api.external.kis.client.KisTokenClient;
import com.monow.api.external.kis.dto.response.KisStockInfoResponse;
import com.monow.api.external.kis.dto.response.KisTokenResponse;
import com.monow.api.external.kis.mapper.KisStockInfoMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StockInfoSyncServiceTest {
    private static final String STOCK_CODE = "005930";
    private static final String ACCESS_TOKEN = "access_token";

    @Mock
    private StockRepository stockRepository;

    @Mock
    private KisTokenClient kisTokenClient;

    @Mock
    private KisStockInfoClient kisStockInfoClient;

    @Mock
    private KisStockInfoMapper kisStockInfoMapper;

    @InjectMocks
    private StockInfoSyncService stockInfoSyncService;

    @Nested
    @DisplayName("주식 종목 동기화")
    class SyncStockInfo {

        @Test
        @DisplayName("[성공] - 종목 정보를 조회하여 저장")
        void syncStockInfo_whenStockDoesNotExist_savesNewStock() {
            // Given
            KisTokenResponse tokenResponse = new KisTokenResponse(
                    ACCESS_TOKEN,
                    "Bearer",
                    86400L
            );

            KisStockInfoResponse.Output output = new KisStockInfoResponse.Output(
                    "00000A005930",
                    "KR7005930003",
                    STOCK_CODE,
                    "삼성전자보통주",
                    "삼성전자",
                    "300",
                    "101010",
                    "주권",
                    "1010",
                    "주식"
            );

            KisStockInfoResponse response = new KisStockInfoResponse(
                    "0",
                    "MCA0000",
                    "정상처리 되었습니다.",
                    output
            );

            Stock stock = Stock.createStock(
                    "00000A005930",
                    "KR7005930003",
                    STOCK_CODE,
                    "삼성전자보통주",
                    "삼성전자",
                    "DOMESTIC_STOCK",
                    "300",
                    "101010",
                    "주권",
                    "1010",
                    "주식"
            );

            given(stockRepository.existsByStockCode(STOCK_CODE))
                    .willReturn(false);

            given(kisTokenClient.issueToken())
                    .willReturn(tokenResponse);

            given(kisStockInfoClient.fetchStockInfo(ACCESS_TOKEN, STOCK_CODE))
                    .willReturn(response);

            given(kisStockInfoMapper.toEntity(output))

                    .willReturn(stock);

            // When
            stockInfoSyncService.syncStockInfo(STOCK_CODE);

            // Then
            verify(stockRepository).existsByStockCode(STOCK_CODE);
            verify(kisTokenClient).issueToken();
            verify(kisStockInfoClient).fetchStockInfo(ACCESS_TOKEN, STOCK_CODE);
            verify(kisStockInfoMapper).toEntity(output);
            verify(stockRepository).save(stock);

        }

        @Test
        @DisplayName("[스킵] - 이미 저장된 종목이면 API 조회와 저장을 수행하지 않음")
        void syncStockInfo_whenStockAlreadyExists_skipsApiCallAndSave() {
            // Given
            given(stockRepository.existsByStockCode(STOCK_CODE))
                    .willReturn(true);

            // When
            stockInfoSyncService.syncStockInfo(STOCK_CODE);

            // Then
            verify(stockRepository).existsByStockCode(STOCK_CODE);
            verify(kisTokenClient, never()).issueToken();
            verify(kisStockInfoClient, never()).fetchStockInfo(any(), any());
            verify(kisStockInfoMapper, never()).toEntity(any());
            verify(stockRepository, never()).save(any());

        }

        @Test
        @DisplayName("[예외] - KIS 종목 정보 응답 코드가 실패면 Stock을 저장하지 않음")
        void syncStockInfo_whenKisResponseFails_doesNotSaveStock() {
            // Given
            given(stockRepository.existsByStockCode(STOCK_CODE))
                    .willReturn(false);
            KisTokenResponse tokenResponse = new KisTokenResponse(
                    ACCESS_TOKEN,
                    "Bearer",
                    86400L);
            KisStockInfoResponse failResponse= new KisStockInfoResponse(
                    "1",
                    "ERROR_CODE",
                    "종목 정보 조회 실패",
                    null
            );
            given(kisTokenClient.issueToken())
                    .willReturn(tokenResponse);

            given(kisStockInfoClient.fetchStockInfo(ACCESS_TOKEN, STOCK_CODE))
                    .willReturn(failResponse);

            // When & Then
            assertThatThrownBy(() -> stockInfoSyncService.syncStockInfo(STOCK_CODE))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode())
                                .isEqualTo(ErrorCode.STOCK_INFO_FETCH_FAILED);
                    });
            verify(stockRepository).existsByStockCode(STOCK_CODE);
            verify(kisTokenClient).issueToken();
            verify(kisStockInfoClient).fetchStockInfo(ACCESS_TOKEN, STOCK_CODE);
            verify(kisStockInfoMapper, never()).toEntity(any());
            verify(stockRepository, never()).save(any());

        }

        @Test
        @DisplayName("[예외] - KIS 응답은 성공이지만 output이 없으면 Stock을 저장하지 않음")
        void syncStockInfo_whenResponseOutputIsNull_doesNotSaveStock() {
            // Given
            given(stockRepository.existsByStockCode(STOCK_CODE))
                    .willReturn(false);
            KisTokenResponse tokenResponse = new KisTokenResponse(
                    ACCESS_TOKEN,
                    "Bearer",
                    86400L);
            KisStockInfoResponse response= new KisStockInfoResponse(
                    "0",
                    "MCA0000",
                    "정상처리 되었습니다.",
                    null
            );
            given(kisTokenClient.issueToken())
                    .willReturn(tokenResponse);

            given(kisStockInfoClient.fetchStockInfo(ACCESS_TOKEN, STOCK_CODE))
                    .willReturn(response);

            // When & Then
            assertThatThrownBy(() -> stockInfoSyncService.syncStockInfo(STOCK_CODE))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode())
                                .isEqualTo(ErrorCode.STOCK_INFO_FETCH_FAILED);
                    });
            verify(stockRepository).existsByStockCode(STOCK_CODE);
            verify(kisTokenClient).issueToken();
            verify(kisStockInfoClient).fetchStockInfo(ACCESS_TOKEN, STOCK_CODE);
            verify(kisStockInfoMapper, never()).toEntity(any());
            verify(stockRepository, never()).save(any());

        }

    }
}
