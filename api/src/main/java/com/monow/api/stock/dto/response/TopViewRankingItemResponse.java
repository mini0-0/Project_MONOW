package com.monow.api.stock.dto.response;

import com.monow.domain.stock.entity.Stock;

public record TopViewRankingItemResponse(
        int rank,
        String marketCode,
        String stockCode,

        String stockName
) {
}
