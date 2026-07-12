package com.monow.api.stock.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.dto.response.RealtimeStockPriceResponse;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockRealtimePricePublisher {

    private static final String STOCK_TOPIC_PREFIX = "/topic/stocks/";

    private final SimpMessagingTemplate simpMessagingTemplate;

    public void publishCurrentPrice(
            CurrentPriceMarketType marketType,
            String stockCode,
            RealtimeStockPriceResponse response
    ) {
        String topic = STOCK_TOPIC_PREFIX
                 + marketType.name()
                 + "/"
                 + stockCode;

        simpMessagingTemplate.convertAndSend(topic, response);

    }
}
