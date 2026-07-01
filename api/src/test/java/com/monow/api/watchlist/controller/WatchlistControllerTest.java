package com.monow.api.watchlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monow.api.watchlist.application.WatchlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WatchlistController.class)
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WatchlistService watchlistService;


    @Nested
    @DisplayName("관심종목 상태 변경 API")
    class SetWatchlistStatus {

        @Test
        @DisplayName("[성공] - watchlisted=true 요청")
        void setWatchlistStatus_whenWatchlistedTrue_returnsTrueResponse() throws Exception {
            // Given
            String stockCode = "005930";
            Long userId = 1L;

            String requestBody = """
                    {
                        "watchlisted": true
                    }
                    """;

            given(watchlistService.setWatchlistStatus(userId, stockCode, true))
                    .willReturn(true);


            // When & Then
            mockMvc.perform(put("/api/v1/watchlists/{stockCode}/status", stockCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.stockCode").value(stockCode))
                    .andExpect(jsonPath("$.data.watchlisted").value(true));

            verify(watchlistService).setWatchlistStatus(userId, stockCode, true);


        }

        @Test
        @DisplayName("[성공] - watchlisted=false 요청")
        void setWatchlistStatus_whenWatchlistedFalse_returnsFalseResponse() throws Exception {
            // Given
            String stockCode = "005930";
            Long userId = 1L;

            String requestBody = """
                    {
                        "watchlisted": false
                    }
                    """;

            given(watchlistService.setWatchlistStatus(userId, stockCode, false))
                    .willReturn(false);


            // When & Then
            mockMvc.perform(put("/api/v1/watchlists/{stockCode}/status", stockCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.stockCode").value(stockCode))
                    .andExpect(jsonPath("$.data.watchlisted").value(false));

            verify(watchlistService).setWatchlistStatus(userId, stockCode, false);


        }

    }


}