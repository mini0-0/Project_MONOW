package com.monow.api.stock.ranking.dto.response;

import java.util.List;

public record TopViewRankingResponse(
        String rankingType,
        List<TopViewRankingItemResponse> items
) {
}