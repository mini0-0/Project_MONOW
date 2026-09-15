package com.monow.api.trading.order.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.currentprice.application.StockCurrentPriceMarketSelection;
import com.monow.api.stock.currentprice.application.StockCurrentPriceMarketSelector;
import com.monow.api.stock.currentprice.application.StockCurrentPriceQueryService;
import com.monow.api.stock.currentprice.dto.response.StockCurrentPriceResponse;
import com.monow.api.stock.realtime.application.StockRealtimePriceCacheService;
import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockOrderPriceService {

    private final StockCurrentPriceMarketSelector stockCurrentPriceMarketSelector;

    private final StockRealtimePriceCacheService stockRealtimePriceCacheService;

    private final StockCurrentPriceQueryService stockCurrentPriceQueryService;

    public StockOrderPrice getOrderPrice(String stockCode) {
        StockCurrentPriceMarketSelection selection = stockCurrentPriceMarketSelector.select();
        CurrentPriceMarketType marketType = selection.marketType();

        // Redis 실시간 가격 우선 조회
        Optional<StockRealtimePriceResponse> cachedPrice = stockRealtimePriceCacheService.findLatestPriceIfPresent(marketType, stockCode);

        if (cachedPrice.isPresent()) {
            BigDecimal currentPrice = cachedPrice.get().currentPrice();

            validatePrice(currentPrice);

            return new StockOrderPrice(marketType, currentPrice);
        }

        // Redis에 값이 없으면 KIS REST 현재가 조회
        StockCurrentPriceResponse currentPriceResponse = stockCurrentPriceQueryService.getCurrentPrice(stockCode);

        BigDecimal currentPrice = currentPriceResponse.currentPrice();

        validatePrice(currentPrice);

        return new StockOrderPrice(currentPriceResponse.marketType(), currentPrice);

    }

    private void validatePrice(BigDecimal currentPrice) {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.REALTIME_PRICE_INVALID_RESPONSE);
        }
    }


    public record StockOrderPrice(
            CurrentPriceMarketType marketType,
            BigDecimal price
    ) {
    }


}

