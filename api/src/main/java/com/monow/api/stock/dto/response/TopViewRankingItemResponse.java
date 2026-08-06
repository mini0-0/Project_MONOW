package com.monow.api.stock.dto.response;

public record TopViewRankingItemResponse(
        int rank,
        String marketCode,
        String stockCode,

        String stockName
) {
}
