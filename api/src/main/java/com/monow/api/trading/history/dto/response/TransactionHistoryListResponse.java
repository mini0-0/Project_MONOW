package com.monow.api.trading.history.dto.response;

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
