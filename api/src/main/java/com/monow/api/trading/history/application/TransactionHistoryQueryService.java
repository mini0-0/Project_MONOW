package com.monow.api.trading.history.application;

import com.monow.api.trading.history.dto.response.TransactionHistoryDetailResponse;
import com.monow.api.trading.history.dto.response.TransactionHistoryListResponse;
import com.monow.domain.transaction.repository.TransactionHistoryDetailQueryResult;
import com.monow.domain.transaction.repository.TransactionHistoryQueryResult;
import com.monow.domain.transaction.repository.TransactionHistoryRepository;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
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

    public TransactionHistoryDetailResponse getMyDetailTransactionHistory(Long userId, Long transactionHistoryId) {

        TransactionHistoryDetailQueryResult detailHistory = transactionHistoryRepository
                .findByTransactionHistoryId(userId, transactionHistoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_HISTORY_NOT_FOUND));

        return TransactionHistoryDetailResponse.from(detailHistory);
    }
}
