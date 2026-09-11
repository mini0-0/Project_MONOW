package com.monow.api.stock.search.controller;

import com.monow.api.stock.search.application.StockSearchQueryService;
import com.monow.api.stock.search.dto.response.StockSearchResponse;

import com.monow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocks/search")
@RequiredArgsConstructor
public class StockSearchController {

    private final StockSearchQueryService stockSearchQueryService;

    @GetMapping
    public ApiResponse<Page<StockSearchResponse>> searchStocks(
            @RequestParam("keyword") String keyword,
            @PageableDefault(size = 10)Pageable pageable
    ) {

        Page<StockSearchResponse> responses = stockSearchQueryService.searchStocks(keyword, pageable);

        return ApiResponse.success(responses);
    }

}
