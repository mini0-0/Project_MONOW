package com.monow.api.stock.controller;

import com.monow.api.external.kis.application.DomesticStockSyncService;
import com.monow.api.external.kis.application.StockPriceSyncService;
import com.monow.api.stock.admin.controller.StockAdminController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockAdminController.class)
class StockAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DomesticStockSyncService domesticStockSyncService;

    @MockitoBean
    private StockPriceSyncService stockPriceSyncService;

    @Nested
    @DisplayName("관리자 종목 동기화 API")
    class SyncStocks {

        @Test
        @DisplayName("[성공] - 여러 종목코드를 전달하면 선택 종목 동기화를 요청")
        void  givenMultipleStockCodes_whenSyncStocks_thenCallSelectedDomesticStockSync() throws Exception {
            // Given
            String requestBody = """
                    {
                      "stocks": [
                        {
                          "stockCode": "005930",
                          "marketType": "KOSPI"
                        },
                        {
                          "stockCode": "000660",
                          "marketType": "KOSPI"
                        },
                        {
                          "stockCode": "035420",
                          "marketType": "KOSPI"
                        }
                      ]
                    }
                    """;

            // When & Then
            mockMvc.perform(post("/api/v1/admin/stocks/sync")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(domesticStockSyncService)
                    .syncSelectedDomesticStocks(
                            List.of(
                                    "005930",
                                    "000660",
                                    "035420"
                            )
                    );


        }

        @Test
        @DisplayName("[성공] - 국내주식 전체 동기화 요청" )
        void syncDomesticStocks_whenRequested_callsDomesticStockSyncService() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/admin/stocks/domestic/sync"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(domesticStockSyncService).syncDomesticStocks();
        }
    }

}