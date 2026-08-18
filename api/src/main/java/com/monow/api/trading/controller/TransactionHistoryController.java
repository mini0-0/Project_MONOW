package com.monow.api.trading.controller;

import com.monow.api.trading.application.TransactionHistoryQueryService;
import com.monow.api.trading.dto.response.TransactionHistoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/transactions")
@RequiredArgsConstructor
public class TransactionHistoryController {

    private final TransactionHistoryQueryService transactionHistoryQueryService;

    @GetMapping
    public Page<TransactionHistoryListResponse> getMyTransactionHistories(@RequestParam(name = "userId") Long userId, Pageable pageable) {

        return transactionHistoryQueryService.getMyTransactionHistories(userId, pageable);

    }

}
