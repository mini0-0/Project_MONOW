package com.monow.batch.stock.master.exception;

public class DomesticStockMasterSkippableException extends RuntimeException{

    public DomesticStockMasterSkippableException(String message) { super(message);}

    public DomesticStockMasterSkippableException(String message, Throwable cause) { super(message, cause);}
}
