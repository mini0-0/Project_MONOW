package com.monow.batch.stock.dailyprice.application;

import com.monow.external.kis.stock.client.KisDailyPriceClient;
import com.monow.external.kis.stock.dto.response.KisDailyPriceResponse;
import com.monow.external.kis.exception.DailyStockPriceRetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DailyStockPriceRetryService {

    private final KisDailyPriceClient kisDailyPriceClient;

    @Retryable(retryFor = DailyStockPriceRetryableException.class, maxAttempts = 3)
    public KisDailyPriceResponse fetchDailyPrice(String accessToken, String stockCode) {

        return kisDailyPriceClient.fetchDailyPrice(accessToken, stockCode);

    }
}
