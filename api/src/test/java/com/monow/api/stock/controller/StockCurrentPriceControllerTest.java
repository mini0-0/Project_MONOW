package com.monow.api.stock.controller;


import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.StockCurrentPriceQueryService;
import com.monow.api.stock.application.StockRealtimePriceConnectionService;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockCurrentPriceController.class)
public class StockCurrentPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockCurrentPriceQueryService stockCurrentPriceQueryService;

    @MockitoBean
    private StockRealtimePriceConnectionService stockRealtimePriceConnectionService;

    @Nested
    @DisplayName("주식 현재가 조회 API")
    class GetCurrentPrice {

        @Test
        @DisplayName("[성공] - stockCode로 KRX 현재가를 조회")
        void getCurrentPrice_whenStockCodeProvided_returnsCurrentPrice() throws Exception {
            // Given
            CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
            String stockCode = "005930";
            String stockName = "삼성전자";
            String marketName = "KOSPI200";
            String industryName = "전기·전자";

            BigDecimal currentPrice = BigDecimal.valueOf(322500);
            BigDecimal changePrice = BigDecimal.valueOf(23500);
            String changeSign = "2";
            BigDecimal changeRate = BigDecimal.valueOf(7.86);

            Long tradeVolume = 31_006_148L;
            BigDecimal tradeAmount = BigDecimal.valueOf(10_243_164_332_536L);

            BigDecimal openPrice = BigDecimal.valueOf(326000);
            BigDecimal highPrice = BigDecimal.valueOf(339000);
            BigDecimal lowPrice = BigDecimal.valueOf(320000);

            LocalTime tradeTime = LocalTime.of(9, 0, 15);

            LocalDateTime updatedAt = LocalDateTime.of(
                    2026,
                    6,
                    6,
                    9,
                    0,
                    16
            );

            StockCurrentPriceResponse response = new StockCurrentPriceResponse(
                    marketType,
                    stockCode,
                    stockName,
                    marketName,
                    industryName,
                    currentPrice,
                    changePrice,
                    changeSign,
                    changeRate,
                    tradeVolume,
                    tradeAmount,
                    openPrice,
                    highPrice,
                    lowPrice,
                    updatedAt
            );

            given(stockCurrentPriceQueryService.getCurrentPrice(marketType, stockCode))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/stocks/{stockCode}/current-price/{marketType}",
                            stockCode,
                            marketType.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.stockCode").value("005930"))
                    .andExpect(jsonPath("$.data.stockName").value("삼성전자"))
                    .andExpect(jsonPath("$.data.currentPrice").value("322500"));

            verify(stockCurrentPriceQueryService).getCurrentPrice(marketType, stockCode);
        }
    }

    @Test
    @DisplayName("[성공] - 삼성전자 상세 조회 시 stockCode(삼성전자)의 실시간 주가를 받을 수 있도록 등록")
    void getStockDetail_whenSamsungStockRequested_registersStockCodeForRealtimePriceUpdates() throws  Exception{
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String stockCode = "005930";
        String stockName = "삼성전자";
        String marketName = "KOSPI200";
        String industryName = "전기·전자";

        BigDecimal currentPrice = BigDecimal.valueOf(322500);
        BigDecimal changePrice = BigDecimal.valueOf(23500);
        String changeSign = "2";
        BigDecimal changeRate = BigDecimal.valueOf(7.86);

        Long tradeVolume = 31_006_148L;
        BigDecimal tradeAmount = BigDecimal.valueOf(10_243_164_332_536L);

        BigDecimal openPrice = BigDecimal.valueOf(326000);
        BigDecimal highPrice = BigDecimal.valueOf(339000);
        BigDecimal lowPrice = BigDecimal.valueOf(320000);

        LocalTime tradeTime = LocalTime.of(9, 0, 15);
        LocalDateTime updatedAt = LocalDateTime.of(
                2026,
                6,
                6,
                9,
                0,
                16
        );

        StockCurrentPriceResponse response = new StockCurrentPriceResponse(
                marketType,
                stockCode,
                stockName,
                marketName,
                industryName,
                currentPrice,
                changePrice,
                changeSign,
                changeRate,
                tradeVolume,
                tradeAmount,
                openPrice,
                highPrice,
                lowPrice,
                updatedAt
        );


        given(stockCurrentPriceQueryService.getCurrentPrice(marketType, stockCode))
                .willReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/stocks/{stockCode}/detail", stockCode)
                                .param("marketType", marketType.name())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockCode").value("005930"))
                .andExpect(jsonPath("$.data.stockName").value("삼성전자"))
                .andExpect(jsonPath("$.data.webSocketEndpoint").value("/ws"))
                .andExpect(jsonPath("$.data.realtimeTopic").value("/topic/stocks/KRX/005930"));

        verify(stockCurrentPriceQueryService).getCurrentPrice(marketType, stockCode);
        verify(stockRealtimePriceConnectionService).subscribe(marketType, stockCode);


    }

}