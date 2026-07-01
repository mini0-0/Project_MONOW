package com.monow.api.watchlist.controller;

import com.monow.api.watchlist.application.WatchlistService;
import com.monow.api.watchlist.dto.request.WatchlistRequest;
import com.monow.api.watchlist.dto.response.WatchlistResponse;
import com.monow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/watchlists")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @PutMapping("/{stockCode}/status")
    public ApiResponse<WatchlistResponse> setWatchlistStatus(
            @PathVariable(value = "stockCode") String stockCode,
            @RequestBody WatchlistRequest request
    ) {
        // 임시 userId
        Long userId = 2L;
        Boolean watchlisted = watchlistService.setWatchlistStatus(userId, stockCode, request.watchlisted());

        WatchlistResponse response = new WatchlistResponse(
                stockCode,
                watchlisted
        );

        return ApiResponse.success(response);
    }

}
