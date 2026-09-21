package com.monow.api.trading.order.application;

import com.monow.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.currentprice.application.StockCurrentPriceMarketSelection;
import com.monow.api.stock.currentprice.application.StockCurrentPriceMarketSelector;
import com.monow.api.stock.currentprice.application.StockCurrentPriceQueryService;
import com.monow.api.stock.currentprice.application.StockMarketStatus;
import com.monow.api.stock.currentprice.dto.response.StockCurrentPriceResponse;
import com.monow.api.stock.realtime.application.StockRealtimePriceCacheService;
import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockOrderPriceServiceTest {

    private static final String STOCK_CODE = "005930";
    private static final CurrentPriceMarketType MARKET_TYPE = CurrentPriceMarketType.KRX;

    private static final BigDecimal CURRENT_PRICE = BigDecimal.valueOf(352_500);

    @Mock
    private StockCurrentPriceMarketSelector stockCurrentPriceMarketSelector;

    @Mock
    private StockRealtimePriceCacheService stockRealtimePriceCacheService;

    @Mock
    private StockCurrentPriceQueryService stockCurrentPriceQueryService;

    @InjectMocks
    private StockOrderPriceService stockOrderPriceService;


    @Nested
    @DisplayName("주문 가격 조회")
    class GetOrderPrice {

        @Test
        @DisplayName("[성공] - Redis에 현재가가 존재하는 경우 Redis 가격 사용")
        void GivenRedisPriceExists_WhenGetOrderPrice_ThenUseRedisPrice() {
            // Given
            given(stockCurrentPriceMarketSelector.select())
                    .willReturn(createMarketSelection());
            given(stockCurrentPriceQueryService.getCurrentPrice(STOCK_CODE))
                    .willReturn(createCurrentPriceResponse(CURRENT_PRICE));

            // When
            StockOrderPriceService.StockOrderPrice result = stockOrderPriceService.getOrderPrice(STOCK_CODE);

            // Then
            assertThat(result.marketType()).isEqualTo(MARKET_TYPE);
            assertThat(result.price()).isEqualByComparingTo(CURRENT_PRICE);

            verify(stockCurrentPriceQueryService).getCurrentPrice(STOCK_CODE);


        }

    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    @DisplayName("[실패] - Redis 현재가가 0 이하인 경우 예외 발생")
    void GivenInvalidRedisPrice_WhenGetOrderPrice_ThenThrowException(long invalidCurrentPrice) {
        // Given
        BigDecimal currentPrice = BigDecimal.valueOf(invalidCurrentPrice);

        given(stockCurrentPriceMarketSelector.select())
                .willReturn(createMarketSelection());
        given(stockRealtimePriceCacheService.findLatestPriceIfPresent(MARKET_TYPE, STOCK_CODE))
                .willReturn(Optional.of(createRealtimePriceResponse(currentPrice)));

        // When & Then
        assertThatThrownBy(() -> stockOrderPriceService.getOrderPrice(STOCK_CODE))
                .isInstanceOf(BusinessException.class);

        verify(stockCurrentPriceQueryService, never()).getCurrentPrice(any());

    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    @DisplayName("[실패] - REST 현재가가 0 이하인 경우 예외 발생")
    void GivenInvalidRestPrice_WhenGetOrderPrice_ThenThrowException(long invalidCurrentPrice) {
        // Given
        BigDecimal currentPrice = BigDecimal.valueOf(invalidCurrentPrice);

        given(stockCurrentPriceMarketSelector.select()).willReturn(createMarketSelection());
        given(stockRealtimePriceCacheService.findLatestPriceIfPresent(MARKET_TYPE, STOCK_CODE))
                .willReturn(Optional.empty());
        given(stockCurrentPriceQueryService.getCurrentPrice(STOCK_CODE))
                .willReturn(createCurrentPriceResponse(currentPrice));

        // When & Then
        assertThatThrownBy(() -> stockOrderPriceService.getOrderPrice(STOCK_CODE))
                .isInstanceOf(BusinessException.class);

        verify(stockCurrentPriceQueryService).getCurrentPrice(STOCK_CODE);
    }

    @Test
    @DisplayName("[실패] - Redis에 값이 없고, KIS REST 현재가 조회도 실패한 경우 예외 발생")
    void GivenRedisPriceNotFoundAndRestFailed_WhenGetOrderPrice_ThenThrowException() {
        // Given
        given(stockCurrentPriceMarketSelector.select()).willReturn(createMarketSelection());
        given(stockRealtimePriceCacheService.findLatestPriceIfPresent(MARKET_TYPE, STOCK_CODE))
                .willReturn(Optional.empty());
        given(stockCurrentPriceQueryService.getCurrentPrice(STOCK_CODE))
                .willThrow(new BusinessException(ErrorCode.KIS_CURRENT_PRICE_FETCH_FAILED));

        // When & Then
        assertThatThrownBy(() -> stockOrderPriceService.getOrderPrice(STOCK_CODE))
                .isInstanceOf(BusinessException.class);

        verify(stockCurrentPriceQueryService).getCurrentPrice(STOCK_CODE);
    }


    private StockCurrentPriceMarketSelection createMarketSelection() {
        return new StockCurrentPriceMarketSelection(
                MARKET_TYPE,
                StockMarketStatus.OPEN,
                true
        );
    }

    private StockRealtimePriceResponse createRealtimePriceResponse(BigDecimal currentPrice) {
        return new StockRealtimePriceResponse(
                MARKET_TYPE,
                STOCK_CODE,
                currentPrice,
                BigDecimal.valueOf(23_500),
                "2",
                BigDecimal.valueOf(7.86),
                32_006_148L,
                BigDecimal.valueOf(10_243_164_332_536L),
                BigDecimal.valueOf(326_000),
                BigDecimal.valueOf(1_150_000),
                BigDecimal.valueOf(320_000),
                LocalTime.of(9, 0, 15),
                LocalDateTime.of(2026, 6, 6, 9, 0, 16)
        );
    }

    private StockCurrentPriceResponse createCurrentPriceResponse(BigDecimal currentPrice) {
        return new StockCurrentPriceResponse(
                MARKET_TYPE,
                StockMarketStatus.OPEN,
                true,
                STOCK_CODE,
                "삼성전자",
                "KOSPI",
                "전기전자",
                currentPrice,
                BigDecimal.valueOf(23_500),
                "2",
                BigDecimal.valueOf(7.86),
                32_006_148L,
                BigDecimal.valueOf(10_243_164_332_536L),
                BigDecimal.valueOf(326_000),
                BigDecimal.valueOf(1_150_000),
                BigDecimal.valueOf(320_000),
                LocalDateTime.of(2026, 6, 6, 9, 0, 16)
        );
    }


}