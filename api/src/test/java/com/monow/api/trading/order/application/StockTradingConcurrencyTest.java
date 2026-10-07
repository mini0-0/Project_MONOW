package com.monow.api.trading.order.application;

import com.monow.api.trading.order.dto.StockOrderPrice;
import com.monow.domain.account.entity.Account;
import com.monow.domain.account.repository.AccountRepository;
import com.monow.domain.holding.entity.Holding;
import com.monow.domain.holding.repository.HoldingRepository;
import com.monow.domain.order.repository.OrderRepository;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.domain.transaction.repository.TransactionHistoryRepository;
import com.monow.domain.user.entity.User;
import com.monow.domain.user.repository.UserRepository;
import com.monow.external.kis.type.CurrentPriceMarketType;
import com.monow.global.error.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class StockTradingConcurrencyTest {

    private static final String ACCOUNT_NUMBER = "MONOW260101123456";
    private static final String STOCK_CODE = "005930";

    private static final CurrentPriceMarketType MARKET_TYPE = CurrentPriceMarketType.KRX;

    private static final int THREAD_COUNT = 2;

    @Autowired
    private StockTradingTransactionService stockTradingTransactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @BeforeEach
    void setUp() {
        transactionHistoryRepository.deleteAll();
        orderRepository.deleteAll();
        holdingRepository.deleteAll();
        accountRepository.deleteAll();
        stockRepository.deleteAll();
        userRepository.deleteAll();

    }

    @Test
    @DisplayName("[성공] - 동일 계좌 동시 매수 시 잔액을 초과하여 주문되지 않는다")
    void GivenSameAccount_WhenBuyStockConcurrently_ThenPreventOverPurchase() throws Exception {
        // Given
        BigDecimal initialBalance = BigDecimal.valueOf(1_000_000);
        BigDecimal currentPrice = BigDecimal.valueOf(700_000);

        int existingQuantity = 1;
        int buyQuantity = 1;

        BigDecimal existingTotalPurchaseAmount = BigDecimal.valueOf(100_000);

        User savedUser = userRepository.save(createUser());
        Account savedAccount = accountRepository.save(createAccount(savedUser, initialBalance));
        Stock savedStock = stockRepository.save(createStock());
        Holding holding = Holding.createHolding(savedUser, savedAccount, savedStock, existingQuantity, existingTotalPurchaseAmount);

        holdingRepository.save(holding);

        StockOrderPrice orderPrice = new StockOrderPrice(MARKET_TYPE, currentPrice);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();

        // When
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                Future<?> future = executorService.submit(() -> {
                    readyLatch.countDown();

                    try {
                        startLatch.await();
                        stockTradingTransactionService.buyStock(savedUser.getId(), STOCK_CODE, existingQuantity, orderPrice);
                        successCount.incrementAndGet();

                    } catch (BusinessException exception) {
                        failureCount.incrementAndGet();

                        log.info(
                                "동시 매수 실패 - thread={}, message={}",
                                Thread.currentThread().getName(),
                                exception.getMessage()
                        );
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }

                    return null;
                });

                futures.add(future);
            }

            boolean allReady = readyLatch.await(5, TimeUnit.SECONDS);
            assertThat(allReady).isTrue();

            startLatch.countDown();

            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }

        } finally {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        }

        // Then
        Account resultAccount = accountRepository.findByUserId(savedUser.getId())
                .orElseThrow();

        Stock resultStock = stockRepository.findByStockCode(STOCK_CODE)
                .orElseThrow();

        Holding resultHolding = holdingRepository.findByAccountAndStock(resultAccount, resultStock)
                .orElseThrow();

        long orderCount = orderRepository.count();
        long transactionHistoryCount = transactionHistoryRepository.count();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(resultAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(300_000));
        assertThat(resultHolding.getQuantity()).isEqualTo(2);
        assertThat(orderCount).isEqualTo(1);
        assertThat(transactionHistoryCount).isEqualTo(1);

        log.info(
                "동시 매수 테스트 결과 - successCount={}, failureCount={}, finalBalance={}, finalQuantity={}, orderCount={}, transactionHistoryCount={}",
                successCount.get(),
                failureCount.get(),
                resultAccount.getBalance(),
                resultHolding.getQuantity(),
                orderCount,
                transactionHistoryCount
        );

    }

    @Test
    @DisplayName("[성공] - 동일 보유종목 동시 매도 시 보유 수량을 초과하여 주문되지 않는다")
    void GivenSameHolding_WhenSellStockConcurrently_ThenPreventOverSelling() throws Exception {
        // Given
        BigDecimal initialBalance = BigDecimal.valueOf(1_000_000);
        BigDecimal currentPrice = BigDecimal.valueOf(100_000);
        BigDecimal existingTotalPurchaseAmount = BigDecimal.valueOf(1_000_000);

        int existingQuantity = 10;
        int sellQuantity = 7;

        User savedUser = userRepository.save(createUser());
        Account savedAccount = accountRepository.save(createAccount(savedUser, initialBalance));
        Stock savedStock = stockRepository.save(createStock());

        Holding holding = Holding.createHolding(
                savedUser,
                savedAccount,
                savedStock,
                existingQuantity,
                existingTotalPurchaseAmount
        );

        holdingRepository.save(holding);

        StockOrderPrice orderPrice = new StockOrderPrice(MARKET_TYPE, currentPrice);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();

        // When
        try {
            for (int i= 0; i < THREAD_COUNT; i++) {
                Future<?> future = executorService.submit(() -> {
                    readyLatch.countDown();

                    try {
                        startLatch.await();
                        stockTradingTransactionService.sellStock(savedUser.getId(), STOCK_CODE, sellQuantity, orderPrice);
                        successCount.incrementAndGet();

                    } catch (BusinessException exception) {
                        failureCount.incrementAndGet();

                        log.info(
                                "동시 매도 실패 - thread={}, message={}",
                                Thread.currentThread().getName(),
                                exception.getMessage()
                        );

                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);

                    }

                    return null;

                });

                futures.add(future);

            }

            boolean allReady = readyLatch.await(5, TimeUnit.SECONDS);
            assertThat(allReady).isTrue();

            startLatch.countDown();

            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }

        } finally {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        }

        // Then
        Account resultAccount = accountRepository.findByUserId(savedUser.getId()).orElseThrow();
        Stock resultStock = stockRepository.findByStockCode(STOCK_CODE).orElseThrow();
        Holding resultHolding = holdingRepository.findByAccountAndStock(resultAccount, resultStock).orElseThrow();

        long orderCount = orderRepository.count();
        long transactionHistoryCount = transactionHistoryRepository.count();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(resultAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1_700_000));
        assertThat(resultHolding.getQuantity()).isEqualTo(3);
        assertThat(orderCount).isEqualTo(1);
        assertThat(transactionHistoryCount).isEqualTo(1);

        log.info(
                "동시 매도 테스트 결과 - successCount={}, failureCount={}, finalBalance={}, finalQuantity={}, orderCount={}, transactionHistoryCount={}",
                successCount.get(),
                failureCount.get(),
                resultAccount.getBalance(),
                resultHolding.getQuantity(),
                orderCount,
                transactionHistoryCount
        );

    }



    private User createUser() {
        return User.createUser(
                "test@test.com",
                "1234",
                "홍길동",
                "워렌버핏"
        );
    }

    private Account createAccount(
            User user,
            BigDecimal balance
    ) {
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
                true,
                true
        );
    }

}
