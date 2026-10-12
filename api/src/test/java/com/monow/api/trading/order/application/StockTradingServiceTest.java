package com.monow.api.trading.order.application;

import com.monow.api.trading.order.dto.StockOrderPrice;
import com.monow.external.kis.type.CurrentPriceMarketType;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockTradingServiceTest {

    private static final Long USER_ID = 2L;
    private static final String STOCK_CODE = "005930";
    private static final CurrentPriceMarketType MARKET_TYPE = CurrentPriceMarketType.KRX;
    private static final String IDEMPOTENCY_KEY = "order-request-001";

    @Mock
    private StockOrderPriceService stockOrderPriceService;

    @Mock
    private StockTradingTransactionService stockTradingTransactionService;

    @InjectMocks
    private StockTradingService stockTradingService;

    @Nested
    @DisplayName("주식 매수")
    class BuyStock {

        /*
         * 테스트 시나리오
         * 정상적인 수량으로 주식 매수 요청을 하는 경우
         *
         * 실행 흐름
         * 주문 수량 검증
         * 주문 가격 조회
         * 주문 정보와 멱등성 키를 TransactionService에 전달
         *
         * 검증 대상
         * 주문 가격 조회 여부
         * TransactionService 호출 여부
         * idempotencyKey 전달 여부
         */
        @Test
        @DisplayName("[성공] - 정상적인 매수 요청 시 주문 가격 조회 후 거래 처리 호출")
        void GivenValidBuyOrder_WhenBuyStock_ThenDelegateTransaction() {
            // Given
            int quantity = 5;
            BigDecimal currentPrice = BigDecimal.valueOf(352_500);

            StockOrderPrice orderPrice = new StockOrderPrice(MARKET_TYPE, currentPrice);

            given(stockOrderPriceService.getOrderPrice(STOCK_CODE))
                    .willReturn(orderPrice);

            // When
            stockTradingService.buyStock(
                    USER_ID,
                    STOCK_CODE,
                    quantity,
                    IDEMPOTENCY_KEY
            );

            // Then
            verify(stockOrderPriceService).getOrderPrice(STOCK_CODE);

            verify(stockTradingTransactionService).buyStock(
                    USER_ID,
                    STOCK_CODE,
                    quantity,
                    orderPrice,
                    IDEMPOTENCY_KEY
            );
        }

        /*
         * 테스트 시나리오
         * 주문 수량이 0 또는 음수인 상태로 매수 요청을 하는 경우
         *
         * 실행 흐름
         * 주문 수량 검증
         * 잘못된 수량 확인 후 예외 발생
         *
         * 검증 대상
         * BusinessException 발생
         * 주문 가격 조회 미실행
         * TransactionService 호출 미실행
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("[실패] - 주문 수량이 0 이하인 경우 예외 발생")
        void GivenInvalidQuantity_WhenBuyStock_ThenThrowException(int quantity) {
            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(
                            USER_ID,
                            STOCK_CODE,
                            quantity,
                            IDEMPOTENCY_KEY
                    )
            ).isInstanceOf(BusinessException.class);

            verifyNoInteractions(
                    stockOrderPriceService,
                    stockTradingTransactionService
            );
        }

        /*
         * 테스트 시나리오
         * 정상적인 매수 요청이지만 주문 가격 조회에 실패하는 경우
         *
         * 실행 흐름
         * 주문 수량 검증
         * 주문 가격 조회
         * 가격 조회 과정에서 예외 발생
         *
         * 검증 대상
         * BusinessException 발생
         * TransactionService 호출 미실행
         */
        @Test
        @DisplayName("[실패] - 주문 가격 조회에 실패한 경우 거래 처리 미실행")
        void GivenOrderPriceLookupFailed_WhenBuyStock_ThenThrowException() {
            // Given
            int quantity = 5;

            given(stockOrderPriceService.getOrderPrice(STOCK_CODE))
                    .willThrow(new BusinessException(ErrorCode.REALTIME_STOCK_PRICE_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(
                            USER_ID,
                            STOCK_CODE,
                            quantity,
                            IDEMPOTENCY_KEY
                    )
            ).isInstanceOf(BusinessException.class);

            verifyNoInteractions(stockTradingTransactionService);
        }
    }

    @Nested
    @DisplayName("주식 매도")
    class SellStock {

        /*
         * 테스트 시나리오
         * 정상적인 수량으로 주식 매도 요청을 하는 경우
         *
         * 실행 흐름
         * 주문 수량 검증
         * 주문 가격 조회
         * 주문 정보와 멱등성 키를 TransactionService에 전달
         *
         * 검증 대상
         * 주문 가격 조회 여부
         * TransactionService 호출 여부
         * idempotencyKey 전달 여부
         */
        @Test
        @DisplayName("[성공] - 정상적인 매도 요청 시 주문 가격 조회 후 거래 처리 호출")
        void GivenValidSellOrder_WhenSellStock_ThenDelegateTransaction() {
            // Given
            int quantity = 5;
            BigDecimal currentPrice = BigDecimal.valueOf(322_500);

            StockOrderPrice orderPrice = new StockOrderPrice(MARKET_TYPE, currentPrice);

            given(stockOrderPriceService.getOrderPrice(STOCK_CODE))
                    .willReturn(orderPrice);

            // When
            stockTradingService.sellStock(
                    USER_ID,
                    STOCK_CODE,
                    quantity,
                    IDEMPOTENCY_KEY
            );

            // Then
            verify(stockOrderPriceService).getOrderPrice(STOCK_CODE);

            verify(stockTradingTransactionService).sellStock(
                    USER_ID,
                    STOCK_CODE,
                    quantity,
                    orderPrice,
                    IDEMPOTENCY_KEY
            );
        }

        /*
         * 테스트 시나리오
         * 주문 수량이 0 또는 음수인 상태로 매도 요청을 하는 경우
         *
         * 실행 흐름
         * 주문 수량 검증
         * 잘못된 수량 확인 후 예외 발생
         *
         * 검증 대상
         * BusinessException 발생
         * 주문 가격 조회 미실행
         * TransactionService 호출 미실행
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("[실패] - 주문 수량이 0 이하인 경우 예외 발생")
        void GivenInvalidQuantity_WhenSellStock_ThenThrowException(int quantity) {
            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.sellStock(
                            USER_ID,
                            STOCK_CODE,
                            quantity,
                            IDEMPOTENCY_KEY
                    )
            ).isInstanceOf(BusinessException.class);

            verifyNoInteractions(
                    stockOrderPriceService,
                    stockTradingTransactionService
            );
        }

        /*
         * 테스트 시나리오
         * 정상적인 매도 요청이지만 주문 가격 조회에 실패하는 경우
         *
         * 실행 흐름
         * 주문 수량 검증
         * 주문 가격 조회
         * 가격 조회 과정에서 예외 발생
         *
         * 검증 대상
         * BusinessException 발생
         * TransactionService 호출 미실행
         */
        @Test
        @DisplayName("[실패] - 주문 가격 조회에 실패한 경우 거래 처리 미실행")
        void GivenOrderPriceLookupFailed_WhenSellStock_ThenThrowException() {
            // Given
            int quantity = 5;

            given(stockOrderPriceService.getOrderPrice(STOCK_CODE))
                    .willThrow(new BusinessException(ErrorCode.REALTIME_STOCK_PRICE_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.sellStock(
                            USER_ID,
                            STOCK_CODE,
                            quantity,
                            IDEMPOTENCY_KEY
                    )
            ).isInstanceOf(BusinessException.class);

            verifyNoInteractions(stockTradingTransactionService);
        }
    }
}