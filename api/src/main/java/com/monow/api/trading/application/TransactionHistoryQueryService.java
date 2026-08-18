package com.monow.api.trading.application;

import com.monow.api.trading.dto.response.TransactionHistoryListResponse;
import com.monow.domain.transaction.repository.TransactionHistoryQueryResult;
import com.monow.domain.transaction.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionHistoryQueryService {

    private final TransactionHistoryRepository transactionHistoryRepository;

    public Page<TransactionHistoryListResponse> getMyTransactionHistories(Long userId, Pageable pageable) {

        Page<TransactionHistoryQueryResult> histories = transactionHistoryRepository.findByUserId(userId, pageable);

        Page<TransactionHistoryListResponse> responses = histories.map(TransactionHistoryListResponse::from);

        return responses;
    }
}
