package com.monow.api.trading.history.controller;

import com.monow.api.trading.history.application.TransactionHistoryQueryService;
import com.monow.api.trading.history.dto.response.TransactionHistoryDetailResponse;
import com.monow.api.trading.history.dto.response.TransactionHistoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/transactions")
@RequiredArgsConstructor
public class TransactionHistoryController {

    private final TransactionHistoryQueryService transactionHistoryQueryService;

    @GetMapping
    public Page<TransactionHistoryListResponse> getMyTransactionHistories(@RequestParam(name = "userId") Long userId, Pageable pageable) {

        return transactionHistoryQueryService.getMyTransactionHistories(userId, pageable);

    }

    @GetMapping("/{transactionHistoryId}")
    public TransactionHistoryDetailResponse getMyDetailTransactionHistory(@RequestParam(name = "userId") Long userId, @PathVariable(name = "transactionHistoryId") Long transactionHistoryId) {

        return transactionHistoryQueryService.getMyDetailTransactionHistory(userId, transactionHistoryId);
    }


}
