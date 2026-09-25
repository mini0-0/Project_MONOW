package com.monow.external.kis.exception;

public class DailyStockPriceRetryableException extends RuntimeException {

    public DailyStockPriceRetryableException(String message) {
        super(message);
    }

    public DailyStockPriceRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
