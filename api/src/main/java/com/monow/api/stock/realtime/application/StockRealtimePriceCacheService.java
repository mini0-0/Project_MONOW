package com.monow.api.stock.realtime.application;

import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import com.monow.external.kis.type.CurrentPriceMarketType;
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

    private final RedisTemplate<String, StockRealtimePriceResponse> stockRealtimePriceRedisTemplate;

    // WebSocket으로 수신한 최신 실시간 가격 Redis 저장
    public void saveLatestPrice(
            CurrentPriceMarketType marketType,
            String stockCode,
            StockRealtimePriceResponse response
    ) {
        String redisKey = REDIS_KEY_PREFIX + marketType.name() + ":" + stockCode;

        stockRealtimePriceRedisTemplate.opsForValue().set(redisKey, response);

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

        StockRealtimePriceResponse response = stockRealtimePriceRedisTemplate.opsForValue().get(redisKey);

        if (response == null) {
            log.warn(
                    "Redis 실시간 주가 조회 실패 - redisKey={}",
                    redisKey
            );
            throw new BusinessException(ErrorCode.REALTIME_STOCK_PRICE_NOT_FOUND);
        }

        return response;

    }

    // redis에 현재가 조회 없으면 kis를 통한 조회
    public Optional<StockRealtimePriceResponse> findLatestPriceIfPresent(
            CurrentPriceMarketType marketType,
            String stockCode
    ) {
        String redisKey = REDIS_KEY_PREFIX + marketType.name() + ":" + stockCode;

        StockRealtimePriceResponse response = stockRealtimePriceRedisTemplate.opsForValue().get(redisKey);

        return Optional.ofNullable(response);

    }


    // 여러 종목의 최신 실시간 가격 일괄 조회
    public Map<String, StockRealtimePriceResponse> findLatestPrices(CurrentPriceMarketType marketType, List<String> stockCodes) {
        List<String> redisKeys = new ArrayList<>();

        for (String stockCode : stockCodes) {
            String redisKey = REDIS_KEY_PREFIX + marketType.name() + ":" + stockCode;

            redisKeys.add(redisKey);
        }

        List<StockRealtimePriceResponse> cachedValues = stockRealtimePriceRedisTemplate
                .opsForValue()
                .multiGet(redisKeys);

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

            StockRealtimePriceResponse response = cachedValues.get(i);

            if (response == null) {
                throw new BusinessException(ErrorCode.REALTIME_STOCK_PRICE_NOT_FOUND);
            }

            result.put(stockCode, response);

        }

        return result;

    }
}
