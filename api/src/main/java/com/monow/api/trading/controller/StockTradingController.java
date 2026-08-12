package com.monow.api.trading.controller;

import com.monow.api.trading.application.StockTradingService;
import com.monow.api.trading.dto.request.StockOrderRequest;
import com.monow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class StockTradingController {

    private final StockTradingService stockTradingService;

    @PostMapping
    public ApiResponse<Void> orders(@Valid @RequestBody StockOrderRequest request) {

        switch (request.orderType()) {
            case BUY ->
                    stockTradingService.buyStock(request.userId(), request.stockCode(), request.marketType(), request.quantity());


            case SELL ->
                    stockTradingService.sellStock(request.userId(), request.stockCode(), request.marketType(), request.quantity());
        }

        return ApiResponse.success();
    }

}
