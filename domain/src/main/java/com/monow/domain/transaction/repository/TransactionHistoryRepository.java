package com.monow.domain.transaction.repository;

import com.monow.domain.transaction.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

}
