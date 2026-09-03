package com.monow.api.portfolio.dto.response;

import java.util.List;
public record PortfolioResponse(
        PortfolioSummaryResponse summary,
        List<PortfolioHoldingResponse> holdings
) {
}
