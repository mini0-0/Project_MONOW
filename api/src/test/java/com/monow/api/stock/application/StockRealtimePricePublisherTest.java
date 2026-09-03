package com.monow.api.stock.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.realtime.StockRealtimePricePublisher;
import com.monow.api.stock.dto.response.StockRealtimePriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockRealtimePricePublisherTest {

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private StockRealtimePricePublisher stockRealtimePricePublisher;

    @Test
    @DisplayName("[성공] - 실시간 현재가 데이터를 시장 구분(marketType)과 종목 코드(stockCode)에 맞는 topic으로 전송")
    void publishCurrentPrice_whenValidRealtimePrice_sendsToStockTopic() {
        // Given
        CurrentPriceMarketType marketType = CurrentPriceMarketType.KRX;
        String stockCode = "005930";

        BigDecimal currentPrice = BigDecimal.valueOf(322500);
        BigDecimal changePrice = BigDecimal.valueOf(23500);
        String changeSign = "2";
        BigDecimal changeRate = BigDecimal.valueOf(7.86);

        Long tradeVolume = 31_006_148L;
        BigDecimal tradeAmount = BigDecimal.valueOf(10_243_164_332_536L);

        BigDecimal openPrice = BigDecimal.valueOf(326000);
        BigDecimal highPrice = BigDecimal.valueOf(339000);
        BigDecimal lowPrice = BigDecimal.valueOf(320000);

        LocalTime tradeTime = LocalTime.of(9, 0, 15);

        LocalDateTime updatedAt = LocalDateTime.of(
                2026,
                6,
                6,
                9,
                0,
                16
        );
        StockRealtimePriceResponse response = new StockRealtimePriceResponse(
                marketType,
                stockCode,
                currentPrice,
                changePrice,
                changeSign,
                changeRate,
                tradeVolume,
                tradeAmount,
                openPrice,
                highPrice,
                lowPrice,
                tradeTime,
                updatedAt
        );

        String topic = "/topic/stocks/KRX/005930";


        // When
        stockRealtimePricePublisher.publishCurrentPrice(
                marketType,
                stockCode,
                response
        );

        // Then
        verify(simpMessagingTemplate).convertAndSend(topic, response);


    }

}