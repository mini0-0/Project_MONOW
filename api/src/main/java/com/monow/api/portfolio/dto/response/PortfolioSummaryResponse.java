package com.monow.api.portfolio.dto.response;

import java.math.BigDecimal;

public record PortfolioSummaryResponse(
        BigDecimal totalAsset,
        BigDecimal cashBalance,
        BigDecimal totalInvestment,
        BigDecimal stockEvaluationAmount,
        BigDecimal profitLoss,
        BigDecimal profitRate
) {
}
