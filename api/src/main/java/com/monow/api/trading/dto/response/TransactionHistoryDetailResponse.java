package com.monow.api.trading.dto.response;

import com.monow.domain.order.entity.OrderMarketType;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.transaction.entity.TransactionHistoryType;
import com.monow.domain.transaction.repository.TransactionHistoryDetailQueryResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryDetailResponse(
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

    public static TransactionHistoryDetailResponse from(TransactionHistoryDetailQueryResult result) {
        return new TransactionHistoryDetailResponse(
                result.transactionHistoryId(),
                result.stockCode(),
                result.stockName(),
                result.stockMarketType(),
                result.orderMarketType(),
                result.transactionHistoryType(),
                result.quantity(),
                result.price(),
                result.amount(),
                result.beforeBalance(),
                result.afterBalance(),
                result.description(),
                result.createdAt()
        );
    }
}
