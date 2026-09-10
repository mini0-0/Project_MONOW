package com.monow.api.stock.search.controller;

import com.monow.api.stock.search.application.StockSearchQueryService;
import com.monow.api.stock.search.dto.response.StockSearchResponse;

import com.monow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks/search")
@RequiredArgsConstructor
public class StockSearchController {

    private final StockSearchQueryService stockSearchQueryService;

    @GetMapping
    public ApiResponse<List<StockSearchResponse>> searchStocks(@RequestParam("keyword") String keyword) {

        List<StockSearchResponse> responses = stockSearchQueryService.searchStocks(keyword);

        return ApiResponse.success(responses);
    }

}
