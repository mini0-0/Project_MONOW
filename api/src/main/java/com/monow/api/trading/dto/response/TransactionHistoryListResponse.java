package com.monow.api.trading.dto.response;

import com.monow.domain.order.entity.Order;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.transaction.entity.TransactionHistory;
import com.monow.domain.transaction.entity.TransactionHistoryType;
import com.monow.domain.transaction.repository.TransactionHistoryQueryResult;

import java.math.BigDecimal;

public record TransactionHistoryListResponse(
        Long transactionHistoryId,
        String stockCode,
        String stockName,
        TransactionHistoryType transactionHistoryType,
        Integer quantity,
        BigDecimal amount,
        BigDecimal beforeBalance,
        BigDecimal afterBalance
) {
    public static TransactionHistoryListResponse from(TransactionHistoryQueryResult result) {
        return new TransactionHistoryListResponse(
                result.transactionHistoryId(),
                result.stockCode(),
                result.stockName(),
                result.transactionHistoryType(),
                result.quantity(),
                result.amount(),
                result.beforeBalance(),
                result.afterBalance()
        );
    }
}
