package com.monow.api.stock.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.dto.response.RealtimeStockPriceResponse;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

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

        String marketName = "KOSPI200";
        String industryName = "전기·전자";

        String currentPrice = "322500";
        String changePrice = "23500";
        String changeSign = "2";
        String changeRate = "7.86";

        String tradeVolume = "31006148";
        String tradeAmount = "10243164332536";

        String openPrice = "326000";
        String highPrice = "339000";
        String lowPrice = "320000";

        String tradeTime = "09:00:15";

        LocalDateTime updatedAt = LocalDateTime.of(
                2026,
                6,
                6,
                9,
                0,
                16
        );
        RealtimeStockPriceResponse response = new RealtimeStockPriceResponse(
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