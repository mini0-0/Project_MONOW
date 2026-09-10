package com.monow.api.stock.realtime.handler;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import com.monow.api.stock.realtime.application.StockRealtimePriceCacheService;
import com.monow.api.stock.realtime.application.StockRealtimePricePublisher;
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
            StockRealtimePriceResponse response
    ) {
       stockRealtimePriceCacheService.saveLatestPrice(marketType, stockCode, response);
       stockRealtimePricePublisher.publishCurrentPrice(marketType, stockCode,response);
    }
}
