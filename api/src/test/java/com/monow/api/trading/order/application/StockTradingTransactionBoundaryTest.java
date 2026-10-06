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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class StockTradingTransactionBoundaryTest {

    private static final String ACCOUNT_NUMBER = "MONOW260101123456";
    private static final String STOCK_CODE = "005930";
    private static final CurrentPriceMarketType MARKET_TYPE = CurrentPriceMarketType.KRX;
    private static final BigDecimal SEED_MONEY = BigDecimal.valueOf(100_000_000L);


    @Autowired
    private StockTradingService stockTradingService;

    @MockitoBean
    private StockOrderPriceService stockOrderPriceService;

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
    @DisplayName("[성공] - 매수 주문 가격 조회 시 Transaction 비활성 상태 검증")
    void buyStock_WhenGettingOrderPrice_TransactionIsNotActive() {
        // Given
        int quantity = 5;
        BigDecimal currentPrice = BigDecimal.valueOf(250_500);

        User user = createUser();
        Account account = createAccount(user, SEED_MONEY);
        Stock stock = createStock();

        User savedUser = userRepository.save(user);
        accountRepository.save(account);
        stockRepository.save(stock);

        AtomicBoolean transactionActiveDuringPriceLookup = new AtomicBoolean(true);

        given(stockOrderPriceService.getOrderPrice(STOCK_CODE))
                .willAnswer(invocation -> {
                    boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
                    transactionActiveDuringPriceLookup.set(transactionActive);

                    return new StockOrderPrice(MARKET_TYPE, currentPrice);
                });

        // When
        stockTradingService.buyStock(
                savedUser.getId(),
                STOCK_CODE,
                quantity
        );

        // Then
        assertThat(transactionActiveDuringPriceLookup.get()).isFalse();

        verify(stockOrderPriceService).getOrderPrice(STOCK_CODE);
    }


    @Test
    @DisplayName("[성공] - 매도 주문 가격 조회 시 Transaction 비활성 상태 검증")
    void sellStock_WhenGettingOrderPrice_TransactionIsNotActive() {
        // Given
        int existingQuantity = 10;
        int sellQuantity = 5;

        BigDecimal currentPrice = BigDecimal.valueOf(250_500);
        BigDecimal beforeBalance = BigDecimal.valueOf(90_000_000);
        BigDecimal totalPurchaseAmount = BigDecimal.valueOf(3_000_000);

        User savedUser = userRepository.save(createUser());

        Account savedAccount = accountRepository.save(
                createAccount(savedUser, beforeBalance)
        );

        Stock savedStock = stockRepository.save(createStock());

        Holding holding = Holding.createHolding(
                        savedUser,
                        savedAccount,
                        savedStock,
                        existingQuantity,
                        totalPurchaseAmount
                );

        holdingRepository.save(holding);

        AtomicBoolean transactionActiveDuringPriceLookup = new AtomicBoolean(true);

        given(stockOrderPriceService.getOrderPrice(STOCK_CODE))
                .willAnswer(invocation -> {
                    boolean transactionActive =
                            TransactionSynchronizationManager
                                    .isActualTransactionActive();

                    transactionActiveDuringPriceLookup.set(
                            transactionActive
                    );

                    return new StockOrderPrice(
                            MARKET_TYPE,
                            currentPrice
                    );
                });

        // When
        stockTradingService.sellStock(
                savedUser.getId(),
                STOCK_CODE,
                sellQuantity
        );

        // Then
        assertThat(transactionActiveDuringPriceLookup.get())
                .isFalse();

        verify(stockOrderPriceService)
                .getOrderPrice(STOCK_CODE);
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


}
