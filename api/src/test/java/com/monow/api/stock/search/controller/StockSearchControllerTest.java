package com.monow.api.stock.search.controller;

import com.monow.api.stock.search.application.StockSearchQueryService;
import com.monow.api.stock.search.dto.response.StockSearchResponse;
import com.monow.domain.stock.entity.DomesticStockMarketType;

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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockSearchController.class)
class StockSearchControllerTest {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockSearchQueryService stockSearchQueryService;

    @Nested
    @DisplayName("종목 검색")
    class SearchStocks {

        @Test
        @DisplayName("[성공] - keyword로 종목 목록을 조회한다")
        void givenKeyword_whenSearchStocks_thenReturnStockResponses() throws Exception {
            // Given
            String keyword = "삼성";

            StockSearchResponse response1 = new StockSearchResponse(
                    "005930",
                    "삼성전자",
                    DomesticStockMarketType.KOSPI
            );


            StockSearchResponse response2 = new StockSearchResponse(
                    "009150",
                    "삼성전기",
                    DomesticStockMarketType.KOSPI
            );


            Page<StockSearchResponse> responses = new PageImpl<>(
                    List.of(response1, response2),
                    pageable,
                    2
            );

            given(stockSearchQueryService.searchStocks(keyword, pageable))
                    .willReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/v1/stocks/search")
                    .param("keyword",keyword))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.content[0].stockCode").value("005930"))
                    .andExpect(jsonPath("$.data.content[0].stockName").value("삼성전자"))
                    .andExpect(jsonPath("$.data.content[1].stockCode").value("009150"))
                    .andExpect(jsonPath("$.data.content[1].stockName").value("삼성전기"))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.number").value(0));

            verify(stockSearchQueryService).searchStocks(keyword, pageable);
        }
    }

}