package com.monow.api.trading.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.stock.application.StockRealtimePriceCacheService;
import com.monow.api.stock.dto.response.RealtimeStockPriceResponse;
import com.monow.domain.account.entity.Account;
import com.monow.domain.account.repository.AccountRepository;
import com.monow.domain.holding.entity.Holding;
import com.monow.domain.holding.repository.HoldingRepository;
import com.monow.domain.order.entity.Order;
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
import lombok.extern.slf4j.Slf4j;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Slf4j
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
        void buyStock_whenHoldingDoesNotExist_savesNewHoldingAndTransaction() {
            // Given
            int quantity = 5;
            BigDecimal currentPrice = BigDecimal.valueOf(352_500);
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal expectedBalance = SEED_MONEY.subtract(totalAmount);

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();
            RealtimeStockPriceResponse response = createRealtimePriceResponse(currentPrice);

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));

            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));

            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(response);

            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.empty());


            // When
            log.info("주식 매수 실행 전 - currentPrice={}, beforeBalance={}", currentPrice, account.getBalance());
            stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity);
            log.info("주식 매수 실행 후 - currentPrice={}, afterBalance={}", currentPrice, account.getBalance());

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);

            verify(accountRepository).findByUserId(USER_ID);
            verify(stockRepository).findByStockCode(STOCK_CODE);
            verify(stockRealtimePriceCacheService).findLatestPrice(MARKET_TYPE, STOCK_CODE);
            verify(holdingRepository).findByAccountAndStock(account, stock);

            // Holding 저장 확인
            ArgumentCaptor<Holding> holdingCaptor = ArgumentCaptor.forClass(Holding.class);
            verify(holdingRepository).save(holdingCaptor.capture());

            Holding saveHolding = holdingCaptor.getValue();

            assertThat(saveHolding.getQuantity()).isEqualTo(quantity);
            assertThat(saveHolding.getTotalPurchaseAmount()).isEqualByComparingTo(totalAmount);

            // Order 저장 확인
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();

            assertThat(savedOrder.getQuantity()).isEqualTo(quantity);
            assertThat(savedOrder.getOrderPrice()).isEqualByComparingTo(currentPrice);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(totalAmount);

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
         * 사용자가 이미 보유한 주식 종목을 추가로 매수하는 경우
         *
         * 실행 흐름
         * 계좌 조회
         * 종목 조회
         * 실시간 현재가 조회
         * 추가 주문 금액만큼 계좌 잔액 차감
         * 기존 Holding의 보유 수량과 총 매입 금액 누적
         * 신규 주문 및 거래 내역 저장
         *
         * 검증 대상
         * 기존 Holding의 보유 수량 증가
         * 기존 총 매입 금액에 추가 주문 금액 누적
         * 신규 Holding 저장 미발생
         * Order와 TransactionHistory 정상 저장
         */
        @Test
        @DisplayName("[성공] - 이미 보유한 종목 추가 매수 시 보유 수량과 총 매입 금액 갱신")
        void buyStock_whenHoldingExists_updatesHolding() {
            // Given
            int existingPurchaseQuantity = 5;
            int addPurchaseQuantity = 10;

            BigDecimal existingTotalPurchaseAmount = BigDecimal.valueOf(1_700_000);
            BigDecimal beforeBalance = SEED_MONEY.subtract(existingTotalPurchaseAmount);
            BigDecimal currentPrice = BigDecimal.valueOf(322_500);
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(addPurchaseQuantity));
            BigDecimal expectedBalance = beforeBalance.subtract(totalAmount);
            BigDecimal expectedTotalPurchaseAmount = existingTotalPurchaseAmount.add(totalAmount);
            int expectedQuantity = existingPurchaseQuantity + addPurchaseQuantity;

            User user = createUser();
            Account account = createAccount(user, beforeBalance);
            Stock stock = createStock();

            Holding holding = Holding.createHolding(
                    user,
                    account,
                    stock,
                    existingPurchaseQuantity,
                    existingTotalPurchaseAmount
                    );

            RealtimeStockPriceResponse response = createRealtimePriceResponse(currentPrice);

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));

            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));

            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(response);

            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.of(holding));

            // When
            log.info("주식 매수 실행 전 - balance={}, quantity={}, totalPurchaseAmount={}",
                    account.getBalance(), holding.getQuantity(), holding.getTotalPurchaseAmount());
            stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, addPurchaseQuantity);
            log.info("주식 매수 실행 후 - balance={}, quantity={}, totalPurchaseAmount={}",
                    account.getBalance(), holding.getQuantity(), holding.getTotalPurchaseAmount());

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);
            assertThat(holding.getQuantity()).isEqualTo(expectedQuantity);
            assertThat(holding.getTotalPurchaseAmount()).isEqualByComparingTo(expectedTotalPurchaseAmount);

            verify(accountRepository).findByUserId(USER_ID);
            verify(stockRepository).findByStockCode(STOCK_CODE);
            verify(stockRealtimePriceCacheService).findLatestPrice(MARKET_TYPE, STOCK_CODE);
            verify(holdingRepository).findByAccountAndStock(account, stock);
            verify(holdingRepository, never()).save(any(Holding.class));

            // Order 저장 확인
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();

            assertThat(savedOrder.getQuantity()).isEqualTo(addPurchaseQuantity);
            assertThat(savedOrder.getOrderPrice()).isEqualByComparingTo(currentPrice);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedOrder.getOrderType()).isEqualTo(OrderType.BUY);

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
         * 주문 총금액이 계좌 잔액과 정확히 같은 경계값에서 매수하는 경우
         *
         * 실행 흐름
         * 현재가 1,000,000원으로 100주 주문
         * 주문 총금액 100,000,000원 계산
         * 계좌 잔액 전액 차감
         * 신규 보유 종목, 주문, 거래 내역 저장
         *
         * 검증 대상
         * 잔액과 주문 금액이 같을 때 잔액 부족 예외가 발생하지 않음
         * 매수 후 계좌 잔액 0원
         * Holding, Order, TransactionHistory 정상 저장
         */
        @Test
        @DisplayName("[성공] - 계좌 잔액과 동일한 금액으로 매수")
        void buyStock_whenBalanceEqualsTotalAmount_succeeds() {
            // Given
            int quantity = 100;
            BigDecimal currentPrice = BigDecimal.valueOf(1_000_000);
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(quantity));

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();
            RealtimeStockPriceResponse response = createRealtimePriceResponse(currentPrice);

            assertThat(totalAmount).isEqualByComparingTo(SEED_MONEY);


            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));

            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));

            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(response);

            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.empty());

            // When
            log.info("주식 매수 실행 전 - currentPrice={}, beforeBalance={}", currentPrice, account.getBalance());
            stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity);
            log.info("주식 매수 실행 후 - currentPrice={}, afterBalance={}", currentPrice, account.getBalance());

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

            // Holding 저장 확인
            ArgumentCaptor<Holding> holdingCaptor = ArgumentCaptor.forClass(Holding.class);
            verify(holdingRepository).save(holdingCaptor.capture());

            Holding saveHolding = holdingCaptor.getValue();

            assertThat(saveHolding.getQuantity()).isEqualTo(quantity);
            assertThat(saveHolding.getTotalPurchaseAmount()).isEqualByComparingTo(totalAmount);

            // Order 저장 확인
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();

            assertThat(savedOrder.getQuantity()).isEqualTo(quantity);
            assertThat(savedOrder.getOrderPrice()).isEqualByComparingTo(currentPrice);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedOrder.getOrderType()).isEqualTo(OrderType.BUY);

            // TransactionHistory 저장 확인
            ArgumentCaptor<TransactionHistory> transactionCaptor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(transactionCaptor.capture());

            TransactionHistory savedTransaction = transactionCaptor.getValue();

            assertThat(savedTransaction.getAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedTransaction.getBeforeBalance()).isEqualByComparingTo(SEED_MONEY);
            assertThat(savedTransaction.getAfterBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        }

        /*
         * 테스트 시나리오
         * 사용자가 매수 가능한 최소 수량보다 작은 수량을 주문하는 경우
         *
         * 실행 흐름
         * buyStock 진입 직후 주문 수량 검증
         * BusinessException 발생
         * 계좌, 종목, 실시간 현재가 조회 및 저장 로직 미실행
         *
         * 검증 대상
         * 0과 음수 수량에서 BusinessException 발생
         * 모든 저장소와 현재가 조회 서비스 미호출
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("[실패] - 주문 수량이 0이하인 경우 예외 발생")
        void buyStock_whenQuantityIsZeroOrNegative_throwsException(int quantity) {
            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            verify(accountRepository, never()).findByUserId(any());
            verify(stockRepository, never()).findByStockCode(any());
            verify(stockRealtimePriceCacheService, never()).findLatestPrice(any(), any());
            verify(holdingRepository, never()).findByAccountAndStock(any(), any());
            verify(holdingRepository, never()).save(any(Holding.class));
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));
        }

        /*
         * 테스트 시나리오
         * 실시간 현재가 조회에는 성공했지만 응답에 포함된 현재가가 0 이하인 경우
         *
         * 실행 흐름
         * 계좌 조회 성공
         * 종목 조회 성공
         * 실시간 현재가 조회 성공
         * 현재가 유효성 검증 실패
         * 잔액 차감과 보유 종목 처리 미실행
         *
         * 검증 대상
         * 0과 음수 현재가에서 BusinessException 발생
         * Holding 조회와 모든 저장 로직 미실행
         * 계좌 잔액 불변
         */
        @ParameterizedTest
        @ValueSource(longs = {0L, -1L})
        @DisplayName("[실패] - 현재가가 0 이하인 경우 예외 발생")
        void buyStock_whenCurrentPriceIsZeroOrNegative_throwsException(long invalidCurrentPrice) {
            // Given
            int quantity = 5;

            BigDecimal currentPrice = BigDecimal.valueOf(invalidCurrentPrice);

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();
            RealtimeStockPriceResponse response = createRealtimePriceResponse(currentPrice);

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));
            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));
            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(response);

            // When & THen
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            verify(accountRepository).findByUserId(USER_ID);
            verify(stockRepository).findByStockCode(STOCK_CODE);
            verify(stockRealtimePriceCacheService).findLatestPrice(MARKET_TYPE, STOCK_CODE);
            verify(holdingRepository, never()).findByAccountAndStock(any(), any());
            verify(holdingRepository, never()).save(any(Holding.class));
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));

            assertThat(account.getBalance()).isEqualByComparingTo(SEED_MONEY);

        }

        /*
         * 테스트 시나리오
         * 주문 총금액이 사용 가능한 계좌 잔액보다 큰 경우
         *
         * 실행 흐름
         * 계좌, 종목, 실시간 현재가 조회
         * 주문 총금액 110,000,000원 계산
         * 계좌 잔액 100,000,000원보다 큰 금액 차감 요청
         * 잔액 부족 예외 발생
         * 보유 종목 처리와 저장 로직 미실행
         *
         * 검증 대상
         * 주문 총금액이 계좌 잔액보다 큰 테스트 조건
         * BusinessException 발생
         * 계좌 잔액 불변
         * Holding, Order, TransactionHistory 저장 미발생
         */
        @Test
        @DisplayName("[실패] - 계좌 잔액이 부족한 경우 예외 발생")
        void buyStock_whenBalanceIsInsufficient_throwsException() {
            // Given
            int quantity = 100;

            BigDecimal currentPrice = BigDecimal.valueOf(1_100_000);
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(quantity));

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();
            RealtimeStockPriceResponse response = createRealtimePriceResponse(currentPrice);

            assertThat(totalAmount).isGreaterThan(SEED_MONEY);

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));

            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));

            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(response);


            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            verify(accountRepository).findByUserId(USER_ID);
            verify(stockRepository).findByStockCode(STOCK_CODE);
            verify(stockRealtimePriceCacheService).findLatestPrice(MARKET_TYPE, STOCK_CODE);
            verify(holdingRepository, never()).findByAccountAndStock(any(), any());
            verify(holdingRepository, never()).save(any(Holding.class));
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));

            assertThat(account.getBalance()).isEqualByComparingTo(SEED_MONEY);

        }

        /*
         * 테스트 시나리오
         * 계좌와 종목은 존재하지만 Redis에 해당 종목의 실시간 현재가가 없는 경우
         *
         * 실행 흐름
         * 계좌 조회 성공
         * 종목 조회 성공
         * 실시간 현재가 캐시 조회
         * REALTIME_STOCK_PRICE_NOT_FOUND 예외 발생
         * 잔액 차감과 보유 종목 처리 미실행
         *
         * 검증 대상
         * 실시간 현재가 조회 시 BusinessException 발생
         * Holding 조회와 모든 저장 로직 미실행
         * 계좌 잔액 불변
         */
        @Test
        @DisplayName("[실패] - 실시간 현재가 조회에 실패한 경우 예외 발생")
        void buyStock_whenRealtimePriceNotFound_throwsException() {
            // Given
            int quantity = 5;

            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);
            Stock stock = createStock();

            BusinessException exception = new BusinessException(ErrorCode.REALTIME_STOCK_PRICE_NOT_FOUND);

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));

            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));

            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willThrow(exception);



            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.buyStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            verify(accountRepository).findByUserId(USER_ID);
            verify(stockRepository).findByStockCode(STOCK_CODE);
            verify(stockRealtimePriceCacheService).findLatestPrice(MARKET_TYPE, STOCK_CODE);

            assertThat(account.getBalance()).isEqualByComparingTo(SEED_MONEY);

        }


    @Nested
    @DisplayName("주식 매도")
    class SellStock {
        /*
         * 테스트 시나리오
         * 사용자가 보유하고 있는 주식의 일부 수량을 정상적인 실시간 현재가로 매도하는 경우
         *
         * 실행 흐름
         * 계좌 조회
         * 종목 조회
         * 실시간 현재가 조회
         * 기존 보유 종목 조회
         * 매도 수량만큼 보유 수량 감소
         * 매도 총 금액만큼 계좌 잔액 증가
         * 매도 주문 및 거래 내역 저장
         *
         * 검증 대상
         * 계좌 잔액 정상 증가
         * Holding의 보유 수량 정상 감소
         * Order의 주문 수량, 주문 가격, 총 주문 금액, 주문 유형
         * TransactionHistory의 거래 금액과 거래 전후 잔액
         */
        @Test
        @DisplayName("[성공] - 보유 종목 일부 매도 시 보유 수량과 계좌 잔액 갱신")
        void sellStock_partialQuantity_success() {
            // Given
            int existingQuantity = 10;
            int sellQuantity = 5;

            BigDecimal currentPrice = BigDecimal.valueOf(322_500L);
            BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(sellQuantity));

            BigDecimal existingTotalPurchaseAmount = BigDecimal.valueOf(2_800_000L);

            BigDecimal beforeBalance = BigDecimal.valueOf(93_500_000L);
            BigDecimal expectedBalance = beforeBalance.add(totalAmount);

            int expectedQuantity = existingQuantity - sellQuantity;

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

            RealtimeStockPriceResponse response = createRealtimePriceResponse(currentPrice);

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));

            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));

            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(response);

            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.of(holding));


            // When
            log.info("주식 매도 실행 전 - currentPrice={}, beforeBalance={}", currentPrice, account.getBalance());
            stockTradingService.sellStock(USER_ID, STOCK_CODE, MARKET_TYPE, sellQuantity);
            log.info("주식 매도 실행 후 - currentPrice={}, afterBalance={}", currentPrice, account.getBalance());

            // Then
            assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);
            assertThat(holding.getQuantity()).isEqualTo(expectedQuantity);

            verify(accountRepository).findByUserId(USER_ID);
            verify(stockRepository).findByStockCode(STOCK_CODE);
            verify(stockRealtimePriceCacheService).findLatestPrice(MARKET_TYPE, STOCK_CODE);
            verify(holdingRepository).findByAccountAndStock(account, stock);

            // Holding 저장 확인
            assertThat(holding.getQuantity()).isEqualTo(expectedQuantity);

            verify(holdingRepository, never()).save(any(Holding.class));


            // Order 저장 확인
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();

            assertThat(savedOrder.getQuantity()).isEqualTo(sellQuantity);
            assertThat(savedOrder.getOrderPrice()).isEqualByComparingTo(currentPrice);
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(totalAmount);
            assertThat(savedOrder.getOrderType()).isEqualTo(OrderType.SELL);


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
         * 사용자가 0 또는 음수 수량으로 주식 매도를 요청하는 경우
         *
         * 실행 흐름
         * sellStock 진입 직후 주문 수량 검증
         * INVALID_ORDER_QUANTITY 예외 발생
         * 계좌, 종목, 실시간 현재가, 보유 종목 조회 미실행
         *
         * 검증 대상
         * 0과 음수 수량에서 BusinessException 발생
         * Account, Stock, Holding 조회 미발생
         * 실시간 현재가 조회 미발생
         * Order 및 TransactionHistory 저장 미발생
         */
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("[실패] - 주문 수량이 0 이하인 경우 예외 발생")
        void sellStock_whenQuantityIsZeroOrNegative_throwsException(int quantity) {
            // When & Then
            assertThatThrownBy(() ->
                    stockTradingService.sellStock(USER_ID, STOCK_CODE, MARKET_TYPE, quantity))
                    .isInstanceOf(BusinessException.class);

            verify(accountRepository, never()).findByUserId(USER_ID);
            verify(stockRepository, never()).findByStockCode(STOCK_CODE);
            verify(stockRealtimePriceCacheService, never()).findLatestPrice(any(), any());
            verify(holdingRepository, never()).findByAccountAndStock(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));

        }

        /*
         * 테스트 시나리오
         * 계좌와 종목은 존재하지만 사용자가 해당 종목을 보유하고 있지 않은 경우
         *
         * 실행 흐름
         * 계좌 조회 성공
         * 종목 조회 성공
         * 실시간 현재가 조회 성공
         * 보유 종목 조회 결과 없음
         * HOLDING_NOT_FOUND 예외 발생
         * 매도 처리와 저장 로직 미실행
         *
         * 검증 대상
         * 보유 종목이 없는 경우 BusinessException 발생
         * Account, Stock, 실시간 현재가, Holding 조회 정상 수행
         * 계좌 잔액 변경 미발생
         * Order 및 TransactionHistory 저장 미발생
         */
        @Test
        @DisplayName("[실패] - 보유하지 않은 종목을 매도하는 경우 예외 발생")
        void sellStock_whenHoldingNotFound_throwsException() {
            // Given
            int sellQuantity = 5;

            BigDecimal currentPrice = BigDecimal.valueOf(322_500L);
            BigDecimal beforeBalance = BigDecimal.valueOf(93_500_000L);

            User user = createUser();
            Account account = createAccount(user, beforeBalance);
            Stock stock = createStock();

            RealtimeStockPriceResponse response = createRealtimePriceResponse(currentPrice);

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));

            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));

            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(response);

            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.empty());


            // When
            assertThatThrownBy(() ->
                    stockTradingService.sellStock(USER_ID, STOCK_CODE, MARKET_TYPE, sellQuantity))
                    .isInstanceOf(BusinessException.class);

            // Then
            verify(accountRepository).findByUserId(USER_ID);
            verify(stockRepository).findByStockCode(STOCK_CODE);
            verify(stockRealtimePriceCacheService).findLatestPrice(MARKET_TYPE, STOCK_CODE);
            verify(holdingRepository).findByAccountAndStock(account, stock);

            verify(holdingRepository, never()).save(any(Holding.class));
            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));


        }

        /*
         * 테스트 시나리오
         * 사용자가 실제 보유 수량보다 많은 수량을 매도하려는 경우
         *
         * 실행 흐름
         * 계좌 조회 성공
         * 종목 조회 성공
         * 실시간 현재가 조회 성공
         * 기존 보유 종목 조회 성공
         * Holding의 매도 수량 검증 실패
         * 매도 처리 중단 및 예외 발생
         *
         * 검증 대상
         * 보유 수량 초과 매도 요청 시 BusinessException 발생
         * Holding의 기존 보유 수량과 총 매입 금액 불변
         * 계좌 잔액 불변
         * Order 및 TransactionHistory 저장 미발생
         */
        @Test
        @DisplayName("[실패] - 보유 수량보다 많은 수량을 매도하는 경우 예외 발생")
        void sellStock_whenSellQuantityExceedsHoldingQuantity_throwsException() {
            // Given
            int existingQuantity = 5;
            int sellQuantity = 10;

            BigDecimal currentPrice = BigDecimal.valueOf(322_500L);
            BigDecimal existingTotalPurchaseAmount = BigDecimal.valueOf(1_400_000L);
            BigDecimal beforeBalance = BigDecimal.valueOf(93_500_000L);

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

            RealtimeStockPriceResponse response =
                    createRealtimePriceResponse(currentPrice);

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));

            given(stockRepository.findByStockCode(STOCK_CODE))
                    .willReturn(Optional.of(stock));

            given(stockRealtimePriceCacheService.findLatestPrice(MARKET_TYPE, STOCK_CODE))
                    .willReturn(response);

            given(holdingRepository.findByAccountAndStock(account, stock))
                    .willReturn(Optional.of(holding));


            // When
            assertThatThrownBy(() ->
                    stockTradingService.sellStock(USER_ID, STOCK_CODE, MARKET_TYPE, sellQuantity))
                    .isInstanceOf(BusinessException.class);

            // Then
            assertThat(holding.getQuantity()).isEqualTo(existingQuantity);
            assertThat(holding.getTotalPurchaseAmount()).isEqualByComparingTo(existingTotalPurchaseAmount);
            assertThat(account.getBalance()).isEqualByComparingTo(beforeBalance);

            verify(accountRepository).findByUserId(USER_ID);
            verify(stockRepository).findByStockCode(STOCK_CODE);
            verify(stockRealtimePriceCacheService).findLatestPrice(MARKET_TYPE, STOCK_CODE);
            verify(holdingRepository).findByAccountAndStock(account, stock);

            verify(orderRepository, never()).save(any(Order.class));
            verify(transactionHistoryRepository, never()).save(any(TransactionHistory.class));
        }



    }

        private User createUser() {
            return User.createUser("test@test.com", "1234", "홍길동", "워렌버핏");
        }

        private Account createAccount(User user, BigDecimal balance) {
            return Account.createAccount(user, ACCOUNT_NUMBER, balance);
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
                    "주식"
            );
        }

        private RealtimeStockPriceResponse createRealtimePriceResponse(BigDecimal currentPrice) {
            BigDecimal changePrice = BigDecimal.valueOf(23_500);
            String changeSign = "2";
            BigDecimal changeRate = BigDecimal.valueOf(7.86);
            Long tradeVolume = 32_006_148L;
            BigDecimal tradeAmount = BigDecimal.valueOf(10_243_164_332_536L);
            BigDecimal openPrice = BigDecimal.valueOf(326_000);
            BigDecimal highPrice = BigDecimal.valueOf(1_150_000);
            BigDecimal lowPrice = BigDecimal.valueOf(320_000);
            LocalTime tradeTime = LocalTime.of(9, 0, 15);
            LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 6, 9, 0, 16);

            return new RealtimeStockPriceResponse(
                    MARKET_TYPE,
                    STOCK_CODE,
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
        }
    }
}