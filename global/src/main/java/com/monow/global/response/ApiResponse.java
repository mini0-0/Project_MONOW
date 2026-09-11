package com.monow.global.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;

    private ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    // data가 있는 성공 응답 생성
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }

    // data가 없는 성공 응답 생성
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null);
    }
}