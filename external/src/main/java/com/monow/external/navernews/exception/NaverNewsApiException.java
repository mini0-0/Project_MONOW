package com.monow.external.navernews.exception;

import lombok.Getter;

@Getter
public class NaverNewsApiException extends RuntimeException {

    private final Integer httpStatus;

    public NaverNewsApiException(Integer httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
