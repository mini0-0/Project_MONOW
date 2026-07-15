package com.monow.api.watchlist.dto.response;

public record WatchlistResponse(
        String stockCode,
        boolean watchlisted
) {
}
