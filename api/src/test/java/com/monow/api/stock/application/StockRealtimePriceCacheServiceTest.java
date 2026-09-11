package com.monow.api.stock.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.realtime.application.StockRealtimePriceCacheService;
import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockRealtimePriceCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private StockRealtimePriceCacheService stockRealtimePriceCacheService;

    @Test
    @DisplayName("[성공] - 실시간 현재가 데이터를 Redis key 규칙에 맞게 저장")
    void saveLatestPrice_whenValidRealtimePrice_savesToRedisWithExpectedKey() {
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

        String redisKey = "stock:price:KRX:005930";

        given(redisTemplate.opsForValue())
                .willReturn(valueOperations);

        // When
        stockRealtimePriceCacheService.saveLatestPrice(marketType, stockCode, response);

        // Then
        verify(valueOperations).set(redisKey, response);


    }

}