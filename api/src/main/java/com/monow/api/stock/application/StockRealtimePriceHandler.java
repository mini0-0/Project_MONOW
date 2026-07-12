package com.monow.api.stock.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.dto.response.RealtimeStockPriceResponse;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockRealtimePriceHandler {

    private final StockRealtimePriceCacheService stockRealtimePriceCacheService;

    private final StockRealtimePricePublisher stockRealtimePricePublisher;

    public void handleRealtimePrice(
            CurrentPriceMarketType marketType,
            String stockCode,
            RealtimeStockPriceResponse response
    ) {
       stockRealtimePriceCacheService.saveLatestPrice(marketType, stockCode, response);
       stockRealtimePricePublisher.publishCurrentPrice(marketType, stockCode,response);
    }
}
