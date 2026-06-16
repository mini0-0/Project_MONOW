package com.monow.api.stock.controller;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.StockCurrentPriceService;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import com.monow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockCurrentPriceController {

    private final StockCurrentPriceService stockCurrentPriceService;

    @GetMapping("/{stockCode}/current-price/{marketType}")
    public ApiResponse<StockCurrentPriceResponse> getCurrentPrice(
            @PathVariable(value = "stockCode") String stockCode,
            @PathVariable(value = "marketType") CurrentPriceMarketType marketType
    ) {
        StockCurrentPriceResponse response = stockCurrentPriceService.getCurrentPrice(stockCode, marketType);

        return ApiResponse.success(response);
    }


}
