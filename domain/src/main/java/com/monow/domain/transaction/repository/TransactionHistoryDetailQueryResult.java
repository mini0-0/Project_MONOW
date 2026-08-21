package com.monow.domain.transaction.repository;

import com.monow.domain.order.entity.OrderMarketType;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.transaction.entity.TransactionHistoryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryDetailQueryResult(
        Long transactionHistoryId,
        String stockCode,
        String stockName,
        DomesticStockMarketType stockMarketType,
        OrderMarketType orderMarketType,
        TransactionHistoryType transactionHistoryType,
        Integer quantity,
        BigDecimal price,
        BigDecimal amount,
        BigDecimal beforeBalance,
        BigDecimal afterBalance,
        String description,
        LocalDateTime createdAt
) {
}
