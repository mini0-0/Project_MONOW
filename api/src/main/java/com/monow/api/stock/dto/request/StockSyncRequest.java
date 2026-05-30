package com.monow.api.stock.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StockSyncRequest(
        @NotEmpty(message = "종목코드는 1개 이상 입력해야 합니다.")
        List<String> stockCodes
) {
}
