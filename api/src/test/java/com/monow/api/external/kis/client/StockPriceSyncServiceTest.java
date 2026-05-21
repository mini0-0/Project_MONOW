package com.monow.api.external.kis.client;

import com.monow.api.external.kis.dto.KisDailyPriceResponse;
import com.monow.api.external.kis.dto.KisTokenResponse;
import com.monow.api.external.kis.mapper.KisDailyPriceMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.api.external.kis.application.StockPriceSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StockPriceSyncServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockPriceDailyRepository stockPriceDailyRepository;

    @Mock
    private KisTokenClient kisTokenClient;

    @Mock
    private KisDailyPriceClient kisDailyPriceClient;

    @Mock
    private KisDailyPriceMapper kisDailyPriceMapper;

    @InjectMocks
    private StockPriceSyncService stockPriceSyncService;

    @Nested
    @DisplayName("일별 시세 동기화")
    class SyncDailyPrice {

        @Test
        @DisplayName("일별 시세를 수집하여 stock_price_daily에 저장")
        void syncDailyPrice_success() {
            // Given

            String accessToken = "acceess_token";
            String stockCode = "005930";
            LocalDate tradeDate = LocalDate.of(2026,5,20);

            Stock stock = Stock.createStock(
                    "005930",
                    "삼성전자",
                    "KOSPI"
            );

            KisTokenResponse tokenResponse = new KisTokenResponse(
                    accessToken,
                    "Bearer",
                    86400L
            );

            KisDailyPriceResponse.Output output = new KisDailyPriceResponse.Output(
                    "20260514",
                    "72000",
                    "73500",
                    "71000",
                    "72800",
                    "12345678"
            );

            KisDailyPriceResponse kisResponse = new KisDailyPriceResponse(
                    "0",
                    "MCA00000",
                    "정상처리 되었습니다.",
                    List.of(output)
            );

            StockPriceDaily stockPriceDaily = StockPriceDaily.createDailyPrice(
                    stock,
                    tradeDate,
                    new BigDecimal("72000"),
                    new BigDecimal("73500"),
                    new BigDecimal("71000"),
                    new BigDecimal("72800"),
                    12345678L
            );

            given(stockRepository.findByStockCode(stockCode))
                    .willReturn(Optional.of(stock));

            given(kisTokenClient.issueToken())
                    .willReturn(tokenResponse);

            given(kisDailyPriceClient.fetchDailyPrice(accessToken, stockCode))
                    .willReturn(kisResponse);

            given(kisDailyPriceMapper.toEntity(stock, output))
                    .willReturn(stockPriceDaily);

            given(stockPriceDailyRepository.existsByStockAndTradeDate(stock, tradeDate))
                    .willReturn(false);


            // When
            stockPriceSyncService.syncDailyPrice(stockCode);

            // Then
            verify(stockRepository).findByStockCode(stockCode);
            verify(kisTokenClient).issueToken();
            verify(kisDailyPriceClient).fetchDailyPrice(accessToken, stockCode);
            verify(kisDailyPriceMapper).toEntity(stock, output);
            verify(stockPriceDailyRepository).existsByStockAndTradeDate(stock, tradeDate);
            verify(stockPriceDailyRepository).save(stockPriceDaily);

            }
    }


}
