package com.monow.api.stock.controller;

import com.monow.api.stock.application.ranking.TopViewRankingService;
import com.monow.api.stock.dto.response.TopViewRankingResponse;
import com.monow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class TopViewRankingController {

    private final TopViewRankingService topViewRankingService;

    @GetMapping("/rankings/top-view")
    public ApiResponse<TopViewRankingResponse> getTopViewRanking(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        TopViewRankingResponse response = topViewRankingService.getTopViewRankStocks(limit);

        return ApiResponse.success(response);
    }
}
