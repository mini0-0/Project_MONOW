package com.monow.api.stock.controller;


import com.monow.api.external.kis.application.DomesticStockSyncService;
import com.monow.api.external.kis.application.StockInfoSyncService;
import com.monow.api.external.kis.application.StockPriceSyncService;
import com.monow.api.external.kis.dto.response.KisStockInfoResponse;
import com.monow.api.stock.dto.request.StockSyncRequest;
import com.monow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stocks")
@RequiredArgsConstructor
public class StockAdminController {

    private final StockInfoSyncService stockInfoSyncService;

    private final DomesticStockSyncService domesticStockSyncService;

    private final StockPriceSyncService stockPriceSyncService;

    // 요청으로 전달받은 특정 종목코드만 수동 동기화
    @PostMapping("/sync")
    public ApiResponse<Void> syncStocks(@RequestBody StockSyncRequest request) {

        for (StockSyncRequest.StockSyncItem stock : request.stocks()) {
            stockInfoSyncService.syncStockInfo(
                    stock.stockCode(),
                    stock.marketType()
            );
        }

        return ApiResponse.success();
    }

    // KIS 국내주식 마스터 파일에서 전체 종목코드를 추출해 동기화
    @PostMapping("/domestic/sync")
    public ApiResponse<Void> syncDomesticStocks() {

        domesticStockSyncService.syncDomesticStocks();

        return ApiResponse.success();
    }

    @PostMapping("/daily-prices/sync")
    public ApiResponse<Void> syncDailyPrices() {
        stockPriceSyncService.syncDailyPrices();

        return ApiResponse.success();
    }

}
