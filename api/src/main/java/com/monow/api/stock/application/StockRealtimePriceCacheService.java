package com.monow.api.stock.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.dto.response.RealtimeStockPriceResponse;
import com.monow.api.stock.dto.response.StockCurrentPriceResponse;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRealtimePriceCacheService {
    private static final String REDIS_KEY_PREFIX = "stock:price:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveLatestPrice(
            CurrentPriceMarketType marketType,
            String stockCode,
            RealtimeStockPriceResponse response
    ) {
        String redisKey = REDIS_KEY_PREFIX + marketType.name() + ":" + stockCode;

        redisTemplate.opsForValue().set(redisKey, response);

        log.info(
                "실시간 주가 Redis 저장 완료 - redisKey={}, currentPrice={}",
                redisKey,
                response.currentPrice()
        );
    }

    public RealtimeStockPriceResponse findLatestPrice(
            CurrentPriceMarketType marketType,
            String stockCode
    ) {
        String redisKey = REDIS_KEY_PREFIX + marketType.name() + ":" + stockCode;

        Object cachedValue = redisTemplate.opsForValue().get(redisKey);

        if (cachedValue == null) {
            log.warn(
                    "Redis 실시간 주가 조회 실패 - redisKey={}",
                    redisKey
            );
            throw new BusinessException(ErrorCode.REALTIME_STOCK_PRICE_NOT_FOUND);
        }

        if (!(cachedValue instanceof RealtimeStockPriceResponse)) {
            log.warn(
                    "Redis 실시간 주가 타입 오류 - redisKey={}, actualType={}",
                    redisKey,
                    cachedValue.getClass().getSimpleName()
            );
            throw  new BusinessException(ErrorCode.REALTIME_PRICE_INVALID_RESPONSE);
        }

        RealtimeStockPriceResponse response = (RealtimeStockPriceResponse) cachedValue;

        return response;

    }
}
