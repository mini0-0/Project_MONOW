package com.monow.api.trading.order.controller;

import com.monow.api.trading.order.application.StockTradingService;
import com.monow.api.trading.order.dto.request.StockOrderRequest;
import com.monow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class StockTradingController {

    private final StockTradingService stockTradingService;

    @PostMapping
    public ApiResponse<Void> createOrder(@Valid @RequestBody StockOrderRequest request) {

        switch (request.orderType()) {
            case BUY ->
                    stockTradingService.buyStock(request.userId(), request.stockCode(), request.marketType(), request.quantity());


            case SELL ->
                    stockTradingService.sellStock(request.userId(), request.stockCode(), request.marketType(), request.quantity());
        }

        return ApiResponse.success();
    }

}
