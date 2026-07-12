package com.monow.api.stock.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.dto.response.RealtimeStockPriceResponse;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockRealtimePriceCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveLatestPrice(
            CurrentPriceMarketType marketType,
            String stockCode,
            RealtimeStockPriceResponse response
    ) {
        String redisKey = "stock:price:" + marketType.name() + ":" + stockCode;
        redisTemplate.opsForValue()
                .set(redisKey, response);
    }
}
