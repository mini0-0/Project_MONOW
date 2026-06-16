package com.monow.api.stock.controller;


import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.StockCurrentPriceService;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    private StockCurrentPriceService stockCurrentPriceService;

    @Nested
    @DisplayName("주식 현재가 조회 API")
    class GetCurrentPrice {

        @Test
        @DisplayName("[성공] - stockCode로 KRX 현재가를 조회")
        void getCurrentPrice_whenStockCodeProvided_returnsCurrentPrice() throws Exception {
            // Given
            String stockCode = "005930";

            StockCurrentPriceResponse response = new StockCurrentPriceResponse(
                    "005930",
                    "삼성전자",
                    "KOSPI200",
                    "전기·전자",
                    "322500",
                    "23500",
                    "2",
                    "7.86",
                    "31006148",
                    "10243164332536",
                    "326000",
                    "339000",
                    "320000",
                    "2026-06-06 00:00:00"
            );

            given(stockCurrentPriceService.getCurrentPrice(stockCode, CurrentPriceMarketType.KRX))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/stocks/{stockCode}/current-price", stockCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.stockCode").value("005930"))
                    .andExpect(jsonPath("$.data.stockName").value("삼성전자"))
                    .andExpect(jsonPath("$.data.currentPrice").value("322500"));

            verify(stockCurrentPriceService).getCurrentPrice(stockCode, CurrentPriceMarketType.KRX);
        }
    }
}