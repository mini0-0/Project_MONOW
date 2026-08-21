package com.monow.api.trading.controller;

import com.monow.api.trading.application.TransactionHistoryQueryService;
import com.monow.api.trading.dto.response.TransactionHistoryDetailResponse;
import com.monow.api.trading.dto.response.TransactionHistoryListResponse;
import com.monow.domain.order.entity.OrderMarketType;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.transaction.entity.TransactionHistoryType;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(TransactionHistoryController.class)
class TransactionHistoryControllerTest {

    private static final Long USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionHistoryQueryService transactionHistoryQueryService;

    private static final String STOCK_CODE = "005930";
    private static final String STOCK_NAME = "삼성전자보통주";

    @Nested
    @DisplayName("내 거래 내역 목록 조회")
    class GetMyTransactionHistories {

        @Test
        @DisplayName("[성공] - 내 거래 내역 목록 조회")
        void getMyTransactionHistories_whenHistoriesExist_returnsOk() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 20);

            TransactionHistoryListResponse buyHistory =
                    new TransactionHistoryListResponse(
                            1L,
                            STOCK_CODE,
                            STOCK_NAME,
                            TransactionHistoryType.BUY,
                            10,
                            BigDecimal.valueOf(3_600_000),
                            BigDecimal.valueOf(100_000_000),
                            BigDecimal.valueOf(96_400_000)
                    );

            TransactionHistoryListResponse sellHistory =
                    new TransactionHistoryListResponse(
                            2L,
                            STOCK_CODE,
                            STOCK_NAME,
                            TransactionHistoryType.SELL,
                            5,
                            BigDecimal.valueOf(1_500_000),
                            BigDecimal.valueOf(96_400_000),
                            BigDecimal.valueOf(97_900_000)
                    );

            Page<TransactionHistoryListResponse> response = new PageImpl<>(List.of(buyHistory, sellHistory), pageable, 2);

            given(transactionHistoryQueryService.getMyTransactionHistories(USER_ID, pageable))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/users/me/transactions")
                            .param("userId", String.valueOf(USER_ID))
                            .param("page", String.valueOf(pageable.getPageNumber()))
                            .param("size", String.valueOf(pageable.getPageSize())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].stockCode").value("005930"))
                    .andExpect(jsonPath("$.content[0].transactionHistoryType").value("BUY"))
                    .andExpect(jsonPath("$.content[1].transactionHistoryType").value("SELL"))
                    .andExpect(jsonPath("$.totalElements").value(2));


            verify(transactionHistoryQueryService).getMyTransactionHistories(USER_ID, pageable);
        }

        @Test
        @DisplayName("[성공] - 거래 내역이 없는 경우 빈 페이지 반환")
        void getMyTransactionHistories_whenHistoriesDoNotExist_returnsEmptyPage() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 20);

            Page<TransactionHistoryListResponse> response = Page.empty(pageable);

            given(transactionHistoryQueryService.getMyTransactionHistories(USER_ID, pageable))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/users/me/transactions")
                            .param("userId", String.valueOf(USER_ID))
                            .param("page", String.valueOf(pageable.getPageNumber()))
                            .param("size", String.valueOf(pageable.getPageSize())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));


            verify(transactionHistoryQueryService).getMyTransactionHistories(USER_ID, pageable);

        }

    }

    @Nested
    @DisplayName("내 거래 내역 상세 조회")
    class GetMyTransactionHistoryDetail {

        @Test
        @DisplayName("[성공] - 거래 내역이 존재하는 경우 상세 내역 조회")
        void getMyTransactionHistoryDetail_whenHistoryExists_returnsOk() throws Exception{
            // Given
            Long transactionHistoryId = 1L;
            DomesticStockMarketType stockMarketType = DomesticStockMarketType.KOSPI;
            OrderMarketType orderMarketType = OrderMarketType.KRX;
            TransactionHistoryType transactionHistoryType = TransactionHistoryType.BUY;
            Integer quantity = 10;
            BigDecimal price = BigDecimal.valueOf(360_000);
            BigDecimal amount = BigDecimal.valueOf(3_600_000);
            BigDecimal beforeBalance = BigDecimal.valueOf(100_000_000);
            BigDecimal afterBalance = BigDecimal.valueOf(96_400_000);
            String description = "삼성전자 10주 매매";

            LocalDateTime createdAt = LocalDateTime.of(
                    2026,
                    8,
                    17,
                    9,
                    0,
                    16
            );

            TransactionHistoryDetailResponse response = new TransactionHistoryDetailResponse(
                    transactionHistoryId,
                    STOCK_CODE,
                    STOCK_NAME,
                    stockMarketType,
                    orderMarketType,
                    transactionHistoryType,
                    quantity,
                    price,
                    amount,
                    beforeBalance,
                    afterBalance,
                    description,
                    createdAt
            );

            given(transactionHistoryQueryService.getMyDetailTransactionHistory(USER_ID, transactionHistoryId))
                    .willReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/users/me/transactions/{transactionHistoryId}", transactionHistoryId)
                            .param("userId", String.valueOf(USER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transactionHistoryId").value(transactionHistoryId))
                    .andExpect(jsonPath("$.stockCode").value(STOCK_CODE))
                    .andExpect(jsonPath("$.stockName").value(STOCK_NAME))
                    .andExpect(jsonPath("$.stockMarketType").value(stockMarketType.name()))
                    .andExpect(jsonPath("$.orderMarketType").value(orderMarketType.name()))
                    .andExpect(jsonPath("$.transactionHistoryType").value(transactionHistoryType.name()))
                    .andExpect(jsonPath("$.quantity").value(quantity))
                    .andExpect(jsonPath("$.price").value(price.intValue()))
                    .andExpect(jsonPath("$.amount").value(amount.intValue()))
                    .andExpect(jsonPath("$.beforeBalance").value(beforeBalance.intValue()))
                    .andExpect(jsonPath("$.afterBalance").value(afterBalance.intValue()))
                    .andExpect(jsonPath("$.description").value(description))
                    .andExpect(jsonPath("$.createdAt").value("2026-08-17T09:00:16"));

            verify(transactionHistoryQueryService).getMyDetailTransactionHistory(USER_ID, transactionHistoryId);

        }

        @Test
        @DisplayName("[실패] - 거래 내역이 존재하지 않는 경우 404 응답")
        void getMyTransactionHistoryDetail_whenHistoryDoesNotExist_returnsNotFound() throws  Exception{
            // Given
            Long transactionHistoryId = 999L;

            given(transactionHistoryQueryService.getMyDetailTransactionHistory(USER_ID, transactionHistoryId))
                    .willThrow(new BusinessException(ErrorCode.TRANSACTION_HISTORY_NOT_FOUND));

            // When & Then
            mockMvc.perform(get("/api/v1/users/me/transactions/{transactionHistoryId}", transactionHistoryId)
                            .param("userId", String.valueOf(USER_ID)))
                    .andExpect(status().isNotFound());

            verify(transactionHistoryQueryService).getMyDetailTransactionHistory(USER_ID, transactionHistoryId);

        }
    }

}