package com.monow.domain.transaction.repository;

import com.monow.domain.transaction.entity.TransactionHistoryType;

import java.math.BigDecimal;

public record TransactionHistoryQueryResult(
        Long transactionHistoryId,
        String stockCode,
        String stockName,
        TransactionHistoryType transactionHistoryType,
        Integer quantity,
        BigDecimal amount,
        BigDecimal beforeBalance,
        BigDecimal afterBalance
) {

}