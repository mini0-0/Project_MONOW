package com.monow.batch.stock.master.application;

import com.monow.external.kis.exception.DomesticStockMasterRetryableException;
import com.monow.external.kis.stock.client.KisStockInfoClient;
import com.monow.external.kis.stock.dto.response.KisStockInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DomesticStockMasterRetryService {

    private final KisStockInfoClient kisStockInfoClient;

    @Retryable(retryFor = DomesticStockMasterRetryableException.class, maxAttempts = 3)
    public KisStockInfoResponse fetchDomesticStockMaster(String accessToken, String stockCode) {

        return kisStockInfoClient.fetchStockInfo(accessToken, stockCode);
    }
}
