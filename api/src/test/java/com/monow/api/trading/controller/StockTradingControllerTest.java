package com.monow.api.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.trading.order.application.StockTradingService;
import com.monow.api.trading.order.dto.request.StockOrderRequest;
import com.monow.api.trading.order.controller.StockTradingController;
import com.monow.domain.order.entity.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockTradingController.class)
class StockTradingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockTradingService stockTradingService;

    @Nested
    @DisplayName("주식 매수")
    class BuyStock {

        @Test
        @DisplayName("[성공] - 정상적인 주식 매수 요청 시 서비스 호출")
        void buyStock_whenRequestIsValid_callsService() throws Exception {
            // Given
            Long userId = 2L;
            String stockCode = "005930";
            CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
            OrderType orderType = OrderType.BUY;
            int quantity = 5;

            StockOrderRequest request = new StockOrderRequest(userId, stockCode, marketType, orderType, quantity);


            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(stockTradingService).buyStock(userId, stockCode, marketType, quantity);

        }

        @Test
        @DisplayName("[실패] - 주문 유형이 없는 경우 잘못된 요청 응답")
        void createOrder_whenOrderTypeIsNull_returnsBadRequest() throws Exception {
            // Given
            Long userId = 2L;
            String stockCode = "005930";
            CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
            OrderType orderType = OrderType.BUY;
            int quantity = 5;

            StockOrderRequest request = new StockOrderRequest(userId, stockCode, marketType, null, quantity);


            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(stockTradingService);
        }

    }

    @Nested
    @DisplayName("주식 매도")
    class SellStock {
        @Test
        @DisplayName("[성공] - 정상적인 주식 매도 요청 시 서비스 호출")
        void sellStock_whenRequestIsValid_callsService() throws Exception {
            // Given
            Long userId = 2L;
            String stockCode = "005930";
            CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
            OrderType orderType = OrderType.SELL;
            int quantity = 5;

            StockOrderRequest request = new StockOrderRequest(userId, stockCode, marketType, orderType, quantity);

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(stockTradingService).sellStock(userId, stockCode, marketType, quantity);


        }
    }

}