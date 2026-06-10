package com.monow.api.stock.dto.response;

import java.util.List;


public record TopViewRankingResponse(
        String rankingType,
        List<TopViewRankingItemResponse> items
) {
}