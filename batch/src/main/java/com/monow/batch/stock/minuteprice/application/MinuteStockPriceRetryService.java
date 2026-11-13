package com.monow.batch.stock.minuteprice.application;

import com.monow.external.kis.auth.application.KisAccessTokenProvider;
import com.monow.external.kis.exception.MinuteStockPriceRetryableException;
import com.monow.external.kis.stock.client.KisMinutePriceClient;
import com.monow.external.kis.stock.dto.request.KisMinutePriceRequest;
import com.monow.external.kis.stock.dto.response.KisMinutePriceResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.concurrent.TimeUnit;

public class MinuteStockPriceRetryService {
    private final KisAccessTokenProvider tokenProvider;
    private final KisMinutePriceClient client;
    private final long intervalNanos;
    private long lastRequestNanos;

    public MinuteStockPriceRetryService(KisAccessTokenProvider tokenProvider, KisMinutePriceClient client,
                                       long requestIntervalMillis) {
        if (requestIntervalMillis < 0) {
            throw new IllegalArgumentException("호출 간격은 음수일 수 없습니다.");
        }
        this.tokenProvider = tokenProvider;
        this.client = client;
        this.intervalNanos = TimeUnit.MILLISECONDS.toNanos(requestIntervalMillis);
    }

    @Retryable(retryFor = MinuteStockPriceRetryableException.class, maxAttempts = 3,
            backoff = @Backoff(delayExpression = "${monow.batch.minute-price.retry-delay-ms:1000}",
                    multiplier = 2, maxDelay = 5_000))
    public synchronized KisMinutePriceResponse fetch(KisMinutePriceRequest request) {
        String token = tokenProvider.getAccessToken();

        if (lastRequestNanos != 0) {
            long remaining = intervalNanos - (System.nanoTime() - lastRequestNanos);
            if (remaining > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(remaining);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("분봉 수집이 중단되었습니다.", exception);
                }
            }
        }

        lastRequestNanos = System.nanoTime();

        return client.fetchMinutePrice(token, request);
    }
}
