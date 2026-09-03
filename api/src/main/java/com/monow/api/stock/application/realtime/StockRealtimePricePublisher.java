package com.monow.api.stock.application.realtime;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.dto.response.StockRealtimePriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockRealtimePricePublisher {

    private static final String STOCK_TOPIC_PREFIX = "/topic/stocks/";

    private final SimpMessagingTemplate simpMessagingTemplate;

    public void publishCurrentPrice(
            CurrentPriceMarketType marketType,
            String stockCode,
            StockRealtimePriceResponse response
    ) {
        String topic = STOCK_TOPIC_PREFIX
                 + marketType.name()
                 + "/"
                 + stockCode;

        simpMessagingTemplate.convertAndSend(topic, response);

        log.info(
                "실시간 주가 STOMP 전송 완료 - topic={}, currentPrice={}",
                topic,
                response.currentPrice()
        );
    }
}
