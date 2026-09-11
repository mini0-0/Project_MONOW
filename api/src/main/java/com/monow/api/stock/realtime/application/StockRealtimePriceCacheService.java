package com.monow.api.stock.realtime.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRealtimePriceCacheService {
    private static final String REDIS_KEY_PREFIX = "stock:price:";

    private final RedisTemplate<String, Object> redisTemplate;

    // WebSocket으로 수신한 최신 실시간 가격 Redis 저장
    public void saveLatestPrice(
            CurrentPriceMarketType marketType,
            String stockCode,
            StockRealtimePriceResponse response
    ) {
        String redisKey = REDIS_KEY_PREFIX + marketType.name() + ":" + stockCode;

        redisTemplate.opsForValue().set(redisKey, response);

        log.info(
                "실시간 주가 Redis 저장 완료 - redisKey={}, currentPrice={}",
                redisKey,
                response.currentPrice()
        );
    }

    // 특정 종목의 최신 실시간 가격 단건 조회
    public StockRealtimePriceResponse findLatestPrice(
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

        if (!(cachedValue instanceof StockRealtimePriceResponse)) {
            log.warn(
                    "Redis 실시간 주가 타입 오류 - redisKey={}, actualType={}",
                    redisKey,
                    cachedValue.getClass().getSimpleName()
            );
            throw  new BusinessException(ErrorCode.REALTIME_PRICE_INVALID_RESPONSE);
        }

        StockRealtimePriceResponse response = (StockRealtimePriceResponse) cachedValue;

        return response;

    }


    // 여러 종목의 최신 실시간 가격 일괄 조회
    public Map<String, StockRealtimePriceResponse> findLatestPrices(CurrentPriceMarketType marketType, List<String> stockCodes) {
        List<String> redisKeys = new ArrayList<>();

        for (String stockCode : stockCodes) {
            String redisKey = REDIS_KEY_PREFIX + marketType.name() + ":" + stockCode;

            redisKeys.add(redisKey);
        }

        List<Object> cachedValues = redisTemplate.opsForValue().multiGet(redisKeys);

        if (cachedValues == null) {
            log.warn(
                    "Redis 실시간 주가 조회 실패 - redisKey={}",
                    redisKeys
            );
            throw new BusinessException(ErrorCode.REALTIME_STOCK_PRICE_NOT_FOUND);
        }

        Map<String, StockRealtimePriceResponse> result = new HashMap<>();

        for (int i = 0; i < stockCodes.size(); i++) {
            String stockCode = stockCodes.get(i);

            Object cachedValue = cachedValues.get(i);

            if (cachedValue == null) {
                throw new BusinessException(ErrorCode.REALTIME_STOCK_PRICE_NOT_FOUND);
            }

            if (!(cachedValue instanceof StockRealtimePriceResponse)) {
                throw new BusinessException(ErrorCode.REALTIME_PRICE_INVALID_RESPONSE);
            }

            StockRealtimePriceResponse response = (StockRealtimePriceResponse) cachedValue;
            result.put(stockCode, response);

        }

        return result;

    }
}
