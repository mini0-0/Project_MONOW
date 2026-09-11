package com.monow.api.portfolio.controller;

import com.monow.api.portfolio.application.PortfolioQueryService;
import com.monow.api.portfolio.dto.response.PortfolioHoldingResponse;
import com.monow.api.portfolio.dto.response.PortfolioResponse;
import com.monow.api.portfolio.dto.response.PortfolioSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import java.util.List;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

    private static final Long USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioQueryService portfolioQueryService;

    @Nested
    @DisplayName("포트폴리오 조회")
    class GetPortfolio {

        @Test
        @DisplayName("[성공] - 포트폴리오 전체 자산 요약과 보유 종목을 조회")
        void givenPortfolioExists_whenGetPortfolio_thenReturnPortfolio() throws Exception {
            // Given
            PortfolioSummaryResponse summaryResponse = new PortfolioSummaryResponse(
                    new BigDecimal("105000000"),  // 총 자산
                    new BigDecimal("19000000"),   // 현금 잔액
                    new BigDecimal("81000000"),   // 총 투자금액
                    new BigDecimal("86000000"),   // 주식 평가금액
                    new BigDecimal("5000000"),    // 평가손익
                    new BigDecimal("6.17")        // 수익률
            );

            PortfolioHoldingResponse samsung = new PortfolioHoldingResponse(
                    "005930",
                    "삼성전자",
                    100,
                    new BigDecimal("36000000"),   // 총 매입금액
                    new BigDecimal("360000"),     // 평균 매수가
                    new BigDecimal("380000"),     // 현재가
                    new BigDecimal("38000000"),   // 평가금액
                    new BigDecimal("2000000"),    // 평가손익
                    new BigDecimal("5.56")        // 수익률
            );

            PortfolioHoldingResponse skHynix = new PortfolioHoldingResponse(
                    "000660",
                    "SK하이닉스",
                    30,
                    new BigDecimal("45000000"),   // 총 매입금액
                    new BigDecimal("1500000"),    // 평균 매수가
                    new BigDecimal("1600000"),    // 현재가
                    new BigDecimal("48000000"),   // 평가금액
                    new BigDecimal("3000000"),    // 평가손익
                    new BigDecimal("6.67")        // 수익률
            );

            PortfolioResponse response = new PortfolioResponse(
                    summaryResponse,
                    List.of(samsung, skHynix)
            );

            given(portfolioQueryService.getPortfolio(USER_ID))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/users/me/portfolio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.summary.totalAsset").value(105000000))
                    .andExpect(jsonPath("$.data.summary.cashBalance").value(19000000))
                    .andExpect(jsonPath("$.data.summary.totalInvestment").value(81000000))
                    .andExpect(jsonPath("$.data.summary.stockEvaluationAmount").value(86000000))
                    .andExpect(jsonPath("$.data.summary.profitLoss").value(5000000))
                    .andExpect(jsonPath("$.data.summary.profitRate").value(6.17))
                    .andExpect(jsonPath("$.data.holdings.length()").value(2))
                    .andExpect(jsonPath("$.data.holdings[0].stockCode").value("005930"))
                    .andExpect(jsonPath("$.data.holdings[0].stockName").value("삼성전자"))
                    .andExpect(jsonPath("$.data.holdings[0].quantity").value(100))
                    .andExpect(jsonPath("$.data.holdings[0].totalPurchaseAmount").value(36000000))
                    .andExpect(jsonPath("$.data.holdings[0].averageBuyPrice").value(360000))
                    .andExpect(jsonPath("$.data.holdings[0].currentPrice").value(380000))
                    .andExpect(jsonPath("$.data.holdings[0].evaluationAmount").value(38000000))
                    .andExpect(jsonPath("$.data.holdings[0].profitLoss").value(2000000))
                    .andExpect(jsonPath("$.data.holdings[0].profitRate").value(5.56))
                    .andExpect(jsonPath("$.data.holdings[1].stockCode").value("000660"))
                    .andExpect(jsonPath("$.data.holdings[1].stockName").value("SK하이닉스"))
                    .andExpect(jsonPath("$.data.holdings[1].currentPrice").value(1600000))
                    .andExpect(jsonPath("$.data.holdings[1].evaluationAmount").value(48000000));

            verify(portfolioQueryService).getPortfolio(USER_ID);

        }

        @Test
        @DisplayName("[성공] - 보유 종목이 없어도 포트폴리오 전체 자산 요약을 조회")
        void givenNoHoldings_whenGetPortfolio_thenReturnEmptyHoldings() throws Exception {
            // Given
            PortfolioSummaryResponse summaryResponse = new PortfolioSummaryResponse(
                    new BigDecimal("100000000"),  // 총 자산
                    new BigDecimal("100000000"),  // 현금 잔액
                    BigDecimal.ZERO,              // 총 투자금액
                    BigDecimal.ZERO,              // 주식 평가금액
                    BigDecimal.ZERO,              // 평가손익
                    BigDecimal.ZERO               // 수익률
            );

            PortfolioResponse response = new PortfolioResponse(
                    summaryResponse,
                    List.of()
            );

            given(portfolioQueryService.getPortfolio(USER_ID))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/users/me/portfolio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.summary.totalAsset").value(100000000))
                    .andExpect(jsonPath("$.data.summary.cashBalance").value(100000000))
                    .andExpect(jsonPath("$.data.summary.totalInvestment").value(0))
                    .andExpect(jsonPath("$.data.summary.stockEvaluationAmount").value(0))
                    .andExpect(jsonPath("$.data.summary.profitLoss").value(0))
                    .andExpect(jsonPath("$.data.summary.profitRate").value(0))
                    .andExpect(jsonPath("$.data.holdings").isEmpty());

            verify(portfolioQueryService).getPortfolio(USER_ID);

        }
    }
}