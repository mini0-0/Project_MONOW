package com.monow.api.trading.controller;

import com.monow.api.trading.application.TransactionHistoryQueryService;
import com.monow.api.trading.dto.response.TransactionHistoryListResponse;
import com.monow.domain.transaction.entity.TransactionHistoryType;
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

    @Nested
    @DisplayName("내 거래 내역 목록 조회")
    class GetMyTransactionHistories {

        @Test
        @DisplayName("[성공] - 내 거래 내역 목록 조회")
        void getMyTransactionHistories_success() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 20);

            TransactionHistoryListResponse buyHistory =
                    new TransactionHistoryListResponse(
                            1L,
                            "005930",
                            "삼성전자보통주",
                            TransactionHistoryType.BUY,
                            10,
                            BigDecimal.valueOf(3_600_000),
                            BigDecimal.valueOf(100_000_000),
                            BigDecimal.valueOf(96_400_000)
                    );

            TransactionHistoryListResponse sellHistory =
                    new TransactionHistoryListResponse(
                            2L,
                            "005930",
                            "삼성전자보통주",
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
        void getMyTransactionHistories_empty() throws Exception {
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

}