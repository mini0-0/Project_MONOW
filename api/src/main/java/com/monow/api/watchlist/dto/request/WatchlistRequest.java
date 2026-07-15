package com.monow.api.watchlist.dto.request;

import jakarta.validation.constraints.NotNull;

public record WatchlistRequest(
        @NotNull(message = "관심종목 상태는 필수입니다.")
        boolean watchlisted
) {
}
