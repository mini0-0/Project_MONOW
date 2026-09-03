package com.monow.api.stock.controller;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.currentprice.StockCurrentPriceQueryService;
import com.monow.api.stock.application.currentprice.StockMarketStatus;
import com.monow.api.stock.application.realtime.StockRealtimePriceConnectionService;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
        @DisplayName("[성공] - stockCode로 현재 시간에 선택된 시장의 현재가 조회")
        void getCurrentPrice_whenStockCodeProvided_returnsCurrentPrice() throws Exception {
            // Given
            String stockCode = "005930";

            StockCurrentPriceResponse response = createCurrentPriceResponse(
                    CurrentPriceMarketType.KRX,
                    StockMarketStatus.OPEN,
                    true
            );

            given(stockCurrentPriceQueryService.getCurrentPrice(stockCode))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/stocks/{stockCode}/current-price", stockCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.marketType").value("KRX"))
                    .andExpect(jsonPath("$.data.marketStatus").value("OPEN"))
                    .andExpect(jsonPath("$.data.realtime").value(true))
                    .andExpect(jsonPath("$.data.stockCode").value("005930"))
                    .andExpect(jsonPath("$.data.stockName").value("삼성전자"))
                    .andExpect(jsonPath("$.data.currentPrice").value(322500));

            verify(stockCurrentPriceQueryService).getCurrentPrice(stockCode);
        }
    }

    @Nested
    @DisplayName("주식 상세 조회 API")
    class GetStockDetail {

        @Test
        @DisplayName("[성공] - 실시간 거래 시간이면 선택된 시장으로 실시간 현재가 수신 등록")
        void getStockDetail_whenRealtimeMarket_registersStockCodeForRealtimePriceUpdates() throws Exception {
            // Given
            String stockCode = "005930";

            StockCurrentPriceResponse response = createCurrentPriceResponse(
                    CurrentPriceMarketType.KRX,
                    StockMarketStatus.OPEN,
                    true
            );

            given(stockCurrentPriceQueryService.getCurrentPrice(stockCode))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/stocks/{stockCode}/detail", stockCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.stockCode").value("005930"))
                    .andExpect(jsonPath("$.data.stockName").value("삼성전자"))
                    .andExpect(jsonPath("$.data.webSocketEndpoint").value("/ws"))
                    .andExpect(jsonPath("$.data.realtimeTopic").value("/topic/stocks/KRX/005930"));

            verify(stockCurrentPriceQueryService).getCurrentPrice(stockCode);

            verify(stockRealtimePriceConnectionService)
                    .subscribe(CurrentPriceMarketType.KRX, stockCode);
        }

        @Test
        @DisplayName("[성공] - 시장 전환 시간이면 실시간 현재가 수신 등록을 하지 않음")
        void getStockDetail_whenRealtimeDisabled_doesNotRegisterRealtimePrice() throws Exception {
            // Given
            String stockCode = "005930";

            StockCurrentPriceResponse response = createCurrentPriceResponse(
                    CurrentPriceMarketType.KRX,
                    StockMarketStatus.TRANSITION,
                    false
            );

            given(stockCurrentPriceQueryService.getCurrentPrice(stockCode))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/stocks/{stockCode}/detail", stockCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(stockRealtimePriceConnectionService, never())
                    .subscribe(CurrentPriceMarketType.KRX, stockCode);
        }
    }

    private StockCurrentPriceResponse createCurrentPriceResponse(
            CurrentPriceMarketType marketType,
            StockMarketStatus marketStatus,
            boolean realtime
    ) {
        return new StockCurrentPriceResponse(
                marketType,
                marketStatus,
                realtime,
                "005930",
                "삼성전자",
                "KOSPI200",
                "전기·전자",
                BigDecimal.valueOf(322500),
                BigDecimal.valueOf(23500),
                "2",
                BigDecimal.valueOf(7.86),
                31_006_148L,
                BigDecimal.valueOf(10_243_164_332_536L),
                BigDecimal.valueOf(326000),
                BigDecimal.valueOf(339000),
                BigDecimal.valueOf(320000),
                LocalDateTime.of(2026, 9, 1, 9, 0, 16)
        );
    }
}