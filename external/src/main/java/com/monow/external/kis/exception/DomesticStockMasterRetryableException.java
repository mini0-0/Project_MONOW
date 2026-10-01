package com.monow.external.kis.exception;

public class DomesticStockMasterRetryableException extends RuntimeException {

    public DomesticStockMasterRetryableException(String message) { super(message);}

    public DomesticStockMasterRetryableException(String message, Throwable cause) { super(message, cause);}
}
