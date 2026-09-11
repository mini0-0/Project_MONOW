package com.monow.domain.transaction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TransactionHistoryRepositoryCustom {

    Page<TransactionHistoryQueryResult> findByUserId(Long userId, Pageable pageable);

    Optional<TransactionHistoryDetailQueryResult> findByTransactionHistoryId(Long userId, Long transactionHistoryId);
}
