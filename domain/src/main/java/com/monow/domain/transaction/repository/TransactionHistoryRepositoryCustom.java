package com.monow.domain.transaction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionHistoryRepositoryCustom {

    Page<TransactionHistoryQueryResult> findByUserId(Long userId, Pageable pageable);
}
