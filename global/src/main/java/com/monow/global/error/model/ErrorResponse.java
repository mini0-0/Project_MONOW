package com.monow.global.error.model;

public record ErrorResponse(
        boolean success,
        ErrorDetail error
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                false,
                new ErrorDetail(
                        errorCode.getCode(),
                        errorCode.getMessage()
                )
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                false,
                new ErrorDetail(
                        errorCode.getCode(),
                        message
                )
        );
    }

    public record ErrorDetail(
            String code,
            String message) {

    }
}
