package com.monow.batch.stock.dailyprice.exception;

public class DailyStockPriceSkippableException extends RuntimeException {

    public DailyStockPriceSkippableException(String message) {
        super(message);
    }

    public DailyStockPriceSkippableException(String message, Throwable cause) {
        super(message, cause);
    }

}
