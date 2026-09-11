package com.monow.api.portfolio.dto.response;

import java.math.BigDecimal;

public record PortfolioHoldingResponse(
        String stockCode,
        String stockName,
        Integer quantity,
        BigDecimal totalPurchaseAmount,
        BigDecimal averageBuyPrice,
        BigDecimal currentPrice,
        BigDecimal evaluationAmount,
        BigDecimal profitLoss,
        BigDecimal profitRate
) {
}
