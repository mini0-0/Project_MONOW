package com.monow.api.trading.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.realtime.StockRealtimePriceCacheService;
import com.monow.api.stock.dto.response.StockRealtimePriceResponse;
import com.monow.api.trading.order.application.StockTradingService;
import com.monow.domain.account.entity.Account;
import com.monow.domain.account.repository.AccountRepository;
import com.monow.domain.holding.entity.Holding;
import com.monow.domain.holding.repository.HoldingRepository;
import com.monow.domain.order.entity.Order;
import com.monow.domain.order.entity.OrderMarketType;
import com.monow.domain.order.entity.OrderType;
import com.monow.domain.order.repository.OrderRepository;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.domain.transaction.entity.TransactionHistory;
import com.monow.domain.transaction.repository.TransactionHistoryRepository;
import com.monow.domain.user.entity.User;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockTradingServiceTest {

    private static final Long USER_ID = 2L;
    private static final String ACCOUNT_NUMBER = "MONOW260101123456";
    private static final String STOCK_CODE = "005930";
    private static final CurrentPriceMarketType MARKET_TYPE = CurrentPriceMarketType.KRX;
    private static final BigDecimal SEED_MONEY = BigDecimal.valueOf(100_000_000L);

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private StockRealtimePriceCacheService stockRealtimePriceCacheService;

    @InjectMocks
    private StockTradingService stockTradingService;

    @Nested
    @DisplayName("주식 매수")
    class BuyStock {

        /*
         * 테스트 시나리오
         * 사용자가 아직 보유하지 않은 주식 종목을 정상 수량과 실시간 현재가로 매수하는 경우
         *
         * 실행 흐름
         * 계좌 조회
         * 종목 조회
         * 실시간 현재가 조회
         * 주문 총 금액만큼 계좌 잔액 차감
         * 신규 보유 종목 생성
         * 주문 및 거래 내역 저장
         *
         * 검증 대상
         * 계좌 잔액 정상 차감
         * 신규 Holding의 보유 수량과 총 매입 금액
         * Order의 주문 수량, 주문 가격, 총 주문 금액
         * TransactionHistory의 거래 금액과 거래 전후 잔액
         */
        @Test
        @DisplayName("[성공] - 보유하지 않은 종목 매수 시 신규 보유 종목과 거래 내역 저장")
        void GivenNoHolding_WhenBuyStock_ThenSaveNewHoldingAndTransaction() {
            // Given
            int quantity = 5;
            BigDecimal currentPrice = BigDecimal.valueOf(352_500);
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal expectedBalance = SEED_MONEY.subtract(totalAmount);

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();
            StockRealtimePriceResponse response = createRealtimePriceResponse(currentPrice);

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(response);
            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.empty());

            // When
            stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity);

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);

            // Holding 저장 확인
            ArgumentCaptor<Holding> holdingCaptor = ArgumentCaptor.forClass(Holding.class);
            verify(holdingRepository).save(holdingCaptor.capture());

            Holding savedHolding = holdingCaptor.getValue();

            assertThat(savedHolding.getQuantity()).isEqualTo(quantity);
            assertThat(savedHolding.getTotalPurchaseAmount()).isEqualByComparingTo(totalAmount);

            // Order 저장 확인
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();

            assertThat(savedOrder.getQuantity()).isEqualTo(quantity);
            assertThat(savedOrder.getOrderPrice()).isEqualByComparingTo(currentPrice);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedOrder.getOrderType()).isEqualTo(OrderType.BUY);
            assertThat(savedOrder.getOrderMarketType()).isEqualTo(OrderMarketType.KRX);

            // TransactionHistory 저장 확인
            ArgumentCaptor<TransactionHistory> transactionCaptor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(transactionCaptor.capture());

            TransactionHistory savedTransaction = transactionCaptor.getValue();
            assertThat(savedTransaction.getAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedTransaction.getBeforeBalance()).isEqualByComparingTo(SEED_MONEY);
            assertThat(savedTransaction.getAfterBalance()).isEqualByComparingTo(expectedBalance);
        }

        /*
         * 테스트 시나리오
         * 사용자가 이미 보유한 종목을 추가 매수하는 경우
         *
         * 실행 흐름
         * 계좌 조회
         * 종목 조회
         * 실시간 현재가 조회
         * 주문 금액만큼 계좌 잔액 차감
         * 기존 Holding의 보유 수량과 총 매입 금액 갱신
         * 주문 및 거래 내역 저장
         *
         * 검증 대상
         * Holding 신규 저장 미발생
         * 기존 보유 수량 증가
         * 총 매입 금액 증가
         * Order와 TransactionHistory 정상 저장
         */
        @Test
        @DisplayName("[성공] - 이미 보유한 종목 추가 매수 시 보유 수량과 총 매입 금액 갱신")
        void GivenExistingHolding_WhenBuyStock_ThenUpdateHolding() {
            // Given
            int existingQuantity = 5;
            int addQuantity = 10;

            BigDecimal existingTotalPurchaseAmount = BigDecimal.valueOf(1_700_000);
            BigDecimal beforeBalance = SEED_MONEY.subtract(existingTotalPurchaseAmount);
            BigDecimal currentPrice = BigDecimal.valueOf(322_500);
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(addQuantity));
            BigDecimal expectedBalance = beforeBalance.subtract(totalAmount);
            BigDecimal expectedTotalPurchaseAmount = existingTotalPurchaseAmount.add(totalAmount);
            int expectedQuantity = existingQuantity + addQuantity;

            User user = createUser();
            Account account = createAccount(user, beforeBalance);
            Stock stock = createStock();

            Holding holding = Holding.createHolding(
                    user,
                    account,
                    stock,
                    existingQuantity,
                    existingTotalPurchaseAmount
            );

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(createRealtimePriceResponse(currentPrice));
            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.of(holding));

            // When
            stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, addQuantity);

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);

            assertThat(holding.getQuantity()).isEqualTo(expectedQuantity);
            assertThat(holding.getTotalPurchaseAmount()).isEqualByComparingTo(expectedTotalPurchaseAmount);

            verify(holdingRepository, never()).save(any(Holding.class));

            // Order 저장 확인
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getQuantity()).isEqualTo(addQuantity);
            assertThat(savedOrder.getOrderPrice()).isEqualByComparingTo(currentPrice);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedOrder.getOrderType()).isEqualTo(OrderType.BUY);
            assertThat(savedOrder.getOrderMarketType()).isEqualTo(OrderMarketType.KRX);

            // TransactionHistory 저장 확인
            ArgumentCaptor<TransactionHistory> transactionCaptor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(transactionCaptor.capture());

            TransactionHistory savedTransaction = transactionCaptor.getValue();
            assertThat(savedTransaction.getAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedTransaction.getBeforeBalance()).isEqualByComparingTo(beforeBalance);
            assertThat(savedTransaction.getAfterBalance()).isEqualByComparingTo(expectedBalance);
        }

        /*
         * 테스트 시나리오
         * 주문 총 금액이 계좌 잔액과 정확히 같은 경우
         *
         * 실행 흐름
         * 현재가 1,000,000원으로 100주 주문
         * 주문 총 금액 100,000,000원 계산
         * 계좌 잔액 전액 차감
         *
         * 검증 대상
         * 잔액 부족 예외 미발생
         * 매수 후 잔액 0원
         * Holding, Order, TransactionHistory 정상 저장
         */
        @Test
        @DisplayName("[성공] - 계좌 잔액과 동일한 금액으로 매수")
        void GivenBalanceEqualsTotalAmount_WhenBuyStock_ThenSucceed() {
            // Given
            int quantity = 100;
            BigDecimal currentPrice = BigDecimal.valueOf(1_000_000);

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(createRealtimePriceResponse(currentPrice));
            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.empty());

            // When
            stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity);

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

            // Holding 저장 확인
            verify(holdingRepository).save(any(Holding.class));

            // Order 저장 확인
            verify(orderRepository).save(any(Order.class));

            // TransactionHistory 저장 확인
            verify(transactionHistoryRepository).save(any(TransactionHistory.class));
        }

        /*
         * 테스트 시나리오
         * 주문 수량이 0 또는 음수인 경우
         *
         * 실행 흐름
         * 주문 수량 검증에서 즉시 예외 발생
         *
         * 검증 대상
         * 이후 Repository 및 실시간 현재가 조회 미실행
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("[실패] - 주문 수량이 0 이하인 경우 예외 발생")
        void GivenInvalidQuantity_WhenBuyStock_ThenThrowException(int quantity) {
            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            verify(accountRepository, never()).findByUserId(any());
            verify(stockRepository, never()).findByStockCode(any());
            verify(stockRealtimePriceCacheService, never()).findLatestPrice(any(), any());
            verify(holdingRepository, never()).findByAccountAndStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));
        }

        /*
         * 테스트 시나리오
         * Redis에서 조회한 실시간 현재가가 0 또는 음수인 경우
         *
         * 실행 흐름
         * 계좌 조회
         * 종목 조회
         * 실시간 현재가 조회
         * 현재가 유효성 검증 실패
         *
         * 검증 대상
         * BusinessException 발생
         * 계좌 잔액 불변
         * 보유 종목 및 주문 저장 미실행
         */
        @ParameterizedTest
        @ValueSource(longs = {0L, -1L})
        @DisplayName("[실패] - 현재가가 0 이하인 경우 예외 발생")
        void GivenInvalidCurrentPrice_WhenBuyStock_ThenThrowException(long invalidCurrentPrice) {
            // Given
            int quantity = 5;
            BigDecimal currentPrice = BigDecimal.valueOf(invalidCurrentPrice);

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(createRealtimePriceResponse(currentPrice));

            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            assertThat(account.getBalance()).isEqualByComparingTo(SEED_MONEY);

            verify(holdingRepository, never()).findByAccountAndStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));
        }

        /*
         * 테스트 시나리오
         * 주문 총 금액이 계좌 잔액보다 큰 경우
         *
         * 실행 흐름
         * 현재가를 기준으로 주문 총 금액 계산
         * Account 잔액 차감 과정에서 잔액 부족 예외 발생
         *
         * 검증 대상
         * 계좌 잔액 불변
         * Holding, Order, TransactionHistory 저장 미발생
         */
        @Test
        @DisplayName("[실패] - 계좌 잔액이 부족한 경우 예외 발생")
        void GivenInsufficientBalance_WhenBuyStock_ThenThrowException() {
            // Given
            int quantity = 100;
            BigDecimal currentPrice = BigDecimal.valueOf(1_100_000);

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(createRealtimePriceResponse(currentPrice));

            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            assertThat(account.getBalance()).isEqualByComparingTo(SEED_MONEY);

            verify(holdingRepository, never()).findByAccountAndStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));
        }

        /*
         * 테스트 시나리오
         * Redis에 해당 종목의 실시간 현재가가 없는 경우
         *
         * 실행 흐름
         * 계좌 조회
         * 종목 조회
         * Redis 현재가 조회 과정에서 예외 발생
         *
         * 검증 대상
         * 계좌 잔액 불변
         * Holding, Order, TransactionHistory 처리 미실행
         */
        @Test
        @DisplayName("[실패] - 실시간 현재가 조회에 실패한 경우 예외 발생")
        void GivenRealtimePriceNotFound_WhenBuyStock_ThenThrowException() {
            // Given
            int quantity = 5;

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willThrow(new BusinessException(ErrorCode.REALTIME_STOCK_PRICE_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            assertThat(account.getBalance()).isEqualByComparingTo(SEED_MONEY);

            verify(holdingRepository, never()).findByAccountAndStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));
        }
    }

    @Nested
    @DisplayName("주식 매도")
    class SellStock {

        /*
         * 테스트 시나리오
         * 사용자가 보유한 종목의 일부 수량을 매도하는 경우
         *
         * 실행 흐름
         * 계좌 조회
         * 종목 조회
         * 실시간 현재가 조회
         * 기존 Holding 조회
         * 보유 수량 감소
         * 매도 금액만큼 계좌 잔액 증가
         * 매도 주문 및 거래 내역 저장
         *
         * 검증 대상
         * 계좌 잔액 증가
         * Holding 수량 감소
         * 매도 Order와 TransactionHistory 정상 저장
         */
        @Test
        @DisplayName("[성공] - 보유 종목 일부 매도 시 보유 수량과 계좌 잔액 갱신")
        void GivenExistingHolding_WhenSellPartialQuantity_ThenUpdateHoldingAndBalance() {
            // Given
            int existingQuantity = 10;
            int sellQuantity = 5;

            BigDecimal currentPrice = BigDecimal.valueOf(322_500);
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(sellQuantity));
            BigDecimal existingTotalPurchaseAmount = BigDecimal.valueOf(2_800_000);
            BigDecimal beforeBalance = BigDecimal.valueOf(93_500_000);
            BigDecimal expectedBalance = beforeBalance.add(totalAmount);

            User user = createUser();
            Account account = createAccount(user, beforeBalance);
            Stock stock = createStock();

            Holding holding = Holding.createHolding(
                    user,
                    account,
                    stock,
                    existingQuantity,
                    existingTotalPurchaseAmount
            );

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(createRealtimePriceResponse(currentPrice));
            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.of(holding));

            // When
            stockTradingService.sellStock(USER_ID, STOCK_CODE, MARKET_TYPE, sellQuantity);

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);

            // Holding 갱신 확인
            assertThat(holding.getQuantity()).isEqualTo(5);

            // Order 저장 확인
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getQuantity()).isEqualTo(sellQuantity);
            assertThat(savedOrder.getOrderPrice()).isEqualByComparingTo(currentPrice);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedOrder.getOrderType()).isEqualTo(OrderType.SELL);
            assertThat(savedOrder.getOrderMarketType()).isEqualTo(OrderMarketType.KRX);

            // TransactionHistory 저장 확인
            ArgumentCaptor<TransactionHistory> transactionCaptor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(transactionCaptor.capture());

            TransactionHistory savedTransaction = transactionCaptor.getValue();
            assertThat(savedTransaction.getAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedTransaction.getBeforeBalance()).isEqualByComparingTo(beforeBalance);
            assertThat(savedTransaction.getAfterBalance()).isEqualByComparingTo(expectedBalance);
        }

        /*
         * 테스트 시나리오
         * 매도 수량이 0 또는 음수인 경우
         *
         * 실행 흐름
         * 주문 수량 검증에서 즉시 예외 발생
         *
         * 검증 대상
         * Repository 및 실시간 현재가 조회 미실행
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("[실패] - 주문 수량이 0 이하인 경우 예외 발생")
        void GivenInvalidQuantity_WhenSellStock_ThenThrowException(int quantity) {
            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.sellStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            verify(accountRepository, never()).findByUserId(any());
            verify(stockRepository, never()).findByStockCode(any());
            verify(stockRealtimePriceCacheService, never()).findLatestPrice(any(), any());
            verify(holdingRepository, never()).findByAccountAndStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));
        }

        /*
         * 테스트 시나리오
         * 사용자가 해당 종목을 보유하지 않은 경우
         *
         * 실행 흐름
         * 계좌 조회
         * 종목 조회
         * 실시간 현재가 조회
         * Holding 조회 실패
         *
         * 검증 대상
         * BusinessException 발생
         * Order와 TransactionHistory 저장 미발생
         */
        @Test
        @DisplayName("[실패] - 보유하지 않은 종목을 매도하는 경우 예외 발생")
        void GivenHoldingNotFound_WhenSellStock_ThenThrowException() {
            // Given
            int sellQuantity = 5;
            BigDecimal beforeBalance = BigDecimal.valueOf(93_500_000);

            User user = createUser();
            Account account = createAccount(user, beforeBalance);
            Stock stock = createStock();

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(createRealtimePriceResponse(BigDecimal.valueOf(322_500)));
            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.sellStock(USER_ID, STOCK_CODE, MARKET_TYPE, sellQuantity))
                    .isInstanceOf(BusinessException.class);

            assertThat(account.getBalance()).isEqualByComparingTo(beforeBalance);

            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));
        }

        /*
         * 테스트 시나리오
         * 매도 수량이 실제 보유 수량보다 많은 경우
         *
         * 실행 흐름
         * Holding 조회
         * holding.sell()에서 보유 수량 검증 실패
         *
         * 검증 대상
         * Holding 수량 불변
         * 계좌 잔액 불변
         * Order와 TransactionHistory 저장 미발생
         */
        @Test
        @DisplayName("[실패] - 보유 수량보다 많은 수량을 매도하는 경우 예외 발생")
        void GivenSellQuantityExceedsHolding_WhenSellStock_ThenThrowException() {
            // Given
            int existingQuantity = 5;
            int sellQuantity = 10;

            BigDecimal beforeBalance = BigDecimal.valueOf(93_500_000);
            BigDecimal existingTotalPurchaseAmount = BigDecimal.valueOf(1_400_000);

            User user = createUser();
            Account account = createAccount(user, beforeBalance);
            Stock stock = createStock();

            Holding holding = Holding.createHolding(
                    user,
                    account,
                    stock,
                    existingQuantity,
                    existingTotalPurchaseAmount
            );

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(createRealtimePriceResponse(BigDecimal.valueOf(322_500)));
            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.of(holding));

            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.sellStock(USER_ID, STOCK_CODE, MARKET_TYPE, sellQuantity))
                    .isInstanceOf(BusinessException.class);

            assertThat(holding.getQuantity()).isEqualTo(existingQuantity);
            assertThat(account.getBalance()).isEqualByComparingTo(beforeBalance);

            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));
        }
    }

    private User createUser() {
        return User.createUser(
                "test@test.com",
                "1234",
                "홍길동",
                "워렌버핏"
        );
    }

    private Account createAccount(User user, BigDecimal balance) {
        return Account.createAccount(
                user,
                ACCOUNT_NUMBER,
                balance
        );
    }

    private Stock createStock() {
        return Stock.createStock(
                "00000A005930",
                "KR7005930003",
                STOCK_CODE,
                "삼성전자보통주",
                "삼성전자",
                DomesticStockMarketType.KOSPI,
                "300",
                "101010",
                "주권",
                "1010",
                "주식",
                true,   // KRX 거래 가능
                true    // NXT 거래 가능
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
}