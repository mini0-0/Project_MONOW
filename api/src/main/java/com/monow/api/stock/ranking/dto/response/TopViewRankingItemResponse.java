package com.monow.api.stock.ranking.dto.response;

public record TopViewRankingItemResponse(
        int rank,
        String marketCode,
        String stockCode,

        String stockName
) {
}
