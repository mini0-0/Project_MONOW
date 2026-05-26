package com.monow.api.external.kis.service;

import com.monow.api.external.kis.application.StockInfoSyncService;
import com.monow.api.external.kis.client.KisStockInfoClient;
import com.monow.api.external.kis.client.KisTokenClient;
import com.monow.api.external.kis.dto.response.KisStockInfoResponse;
import com.monow.api.external.kis.dto.response.KisTokenResponse;
import com.monow.api.external.kis.mapper.KisStockInfoMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StockInfoSyncServiceTest {

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
        @DisplayName("종목 정보를 조회하여 저장")
        void syncStockInfo_success() {
            // Given
            String accessToken = "access_token";
            String stockCode = "005930";

            KisTokenResponse tokenResponse = new KisTokenResponse(
                    accessToken,
                    "Bearer",
                    86400L
            );

            KisStockInfoResponse.Output output = new KisStockInfoResponse.Output(
                    "00000A005930",
                    "KR7005930003",
                    stockCode,
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
                    stockCode,
                    "삼성전자보통주",
                    "삼성전자",
                    "DOMESTIC_STOCK",
                    "300",
                    "101010",
                    "주권",
                    "1010",
                    "주식"
            );

            given(stockRepository.existsByStockCode(stockCode))
                    .willReturn(false);

            given(kisTokenClient.issueToken())
                    .willReturn(tokenResponse);

            given(kisStockInfoClient.fetchStockInfo(accessToken, stockCode))
                    .willReturn(response);

            given(kisStockInfoMapper.toEntity(output))

                    .willReturn(stock);

            // When
            stockInfoSyncService.syncStockInfo(stockCode);

            // Then
            verify(stockRepository).existsByStockCode(stockCode);
            verify(kisTokenClient).issueToken();
            verify(kisStockInfoClient).fetchStockInfo(accessToken, stockCode);
            verify(kisStockInfoMapper).toEntity(output);
            verify(stockRepository).save(stock);

        }
    }
}
