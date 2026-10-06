package com.monow.api.trading.order.application;

import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import com.monow.api.trading.order.dto.StockOrderPrice;
import com.monow.domain.account.entity.Account;
import com.monow.domain.account.repository.AccountRepository;
import com.monow.domain.holding.entity.Holding;
import com.monow.domain.holding.repository.HoldingRepository;
import com.monow.domain.order.entity.Order;
import com.monow.domain.order.repository.OrderRepository;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.domain.transaction.entity.TransactionHistory;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.BDDMockito.any;

@SpringBootTest
@ActiveProfiles("test")
public class StockTradingTransactionTest {

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

    @MockitoSpyBean
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
    @DisplayName("[실패] - 매수 처리 중 예외 발생 시 주문 데이터 전체 Rollback")
    void buyStock_WhenExceptionOccurs_RollbackAllChanges() {
        // Given
        int quantity = 100;
        BigDecimal currentPrice = BigDecimal.valueOf(250_500);
        User user = createUser();
        Account account = createAccount(user, SEED_MONEY);
        Stock samsung = createStock();

        // 주문 실행 전 데이터는 실제 PostgreSQL에 저장
        User savedUser = userRepository.save(user);
        Account savedAccount = accountRepository.save(account);
        Stock savedStock = stockRepository.save(samsung);

        // Redis/KIS 대신 가상의 주문 가격 반환
        given(stockOrderPriceService.getOrderPrice(STOCK_CODE))
                .willReturn(new StockOrderPrice(MARKET_TYPE, currentPrice));

        // 주문 처리 마지막 단계에서 의도적으로 장애 발생
        doThrow(new RuntimeException("거래내역 저장 실패"))
                .when(transactionHistoryRepository)
                .save(any(TransactionHistory.class));


        // When
        assertThatThrownBy(() -> stockTradingService.buyStock(user.getId() , STOCK_CODE, quantity))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("거래내역 저장 실패");;

        // Then
        Account rollbackAccount = accountRepository.findById(savedAccount.getId()).orElseThrow();

        assertThat(rollbackAccount.getBalance()).isEqualByComparingTo(SEED_MONEY);
        assertThat(holdingRepository.findByAccountAndStock(savedAccount, savedStock)).isEmpty();
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(transactionHistoryRepository.findAll()).isEmpty();


    }

    @Test
    @DisplayName("[실패] - 거래내역 저장 실패 시 매도 데이터 전체 Rollback")
    void sellStock_WhenTransactionHistorySaveFails_RollbackAllChanges() {
        // Given
        int existingQuantity = 10;
        int sellQuantity = 5;

        BigDecimal currentPrice = BigDecimal.valueOf(250_500);
        BigDecimal beforeBalance = BigDecimal.valueOf(90_000_000);
        BigDecimal totalPurchaseAmount = BigDecimal.valueOf(3_000_000);

        User user = createUser();
        Account account = createAccount(user, beforeBalance);
        Stock samsung = createStock();

        User savedUser = userRepository.save(user);
        Account savedAccount = accountRepository.save(account);
        Stock savedStock = stockRepository.save(samsung);

        Holding holding = Holding.createHolding(
                user,
                account,
                samsung,
                existingQuantity,
                totalPurchaseAmount
        );

        Holding savedHolding = holdingRepository.save(holding);

        given(stockOrderPriceService.getOrderPrice(STOCK_CODE))
                .willReturn(new StockOrderPrice(MARKET_TYPE, currentPrice));

        doThrow(new RuntimeException("거래내역 저장 실패"))
                .when(transactionHistoryRepository)
                .save(any(TransactionHistory.class));

        // When
        assertThatThrownBy(() -> stockTradingService.sellStock(savedUser.getId(), STOCK_CODE, sellQuantity))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("거래내역 저장 실패");

        // Then
        Account rollbackAccount = accountRepository.findById(savedAccount.getId())
                .orElseThrow();
        assertThat(rollbackAccount.getBalance()).isEqualByComparingTo(beforeBalance);

        Holding rollbackHolding = holdingRepository.findById(savedHolding.getId())
                .orElseThrow();
        assertThat(rollbackHolding.getQuantity()).isEqualTo(existingQuantity);

        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(transactionHistoryRepository.findAll()).isEmpty();


    }

    @Test
    @DisplayName("[성공] - 매수 처리 완료 시 주문 데이터 전체 Commit")
    void buyStock_WhenCompleted_CommitAllChanges() {
        // Given
        int quantity = 5;
        BigDecimal currentPrice = BigDecimal.valueOf(250_500);

        BigDecimal totalAmount  = currentPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal expectedBalance = SEED_MONEY.subtract(totalAmount);

        User user = createUser();
        Account account = createAccount(user, SEED_MONEY);
        Stock samsung = createStock();

        User savedUser = userRepository.save(user);
        Account savedAccount = accountRepository.save(account);
        Stock savedStock = stockRepository.save(samsung);

        given(stockOrderPriceService.getOrderPrice(STOCK_CODE))
                .willReturn(new StockOrderPrice(MARKET_TYPE, currentPrice));

        // When
        stockTradingService.buyStock(savedUser.getId(), STOCK_CODE, quantity);

        // Then
        // Account Commit 확인
        Account committedAccount = accountRepository.findById(savedAccount.getId()).orElseThrow();
        assertThat(committedAccount.getBalance()).isEqualByComparingTo(expectedBalance);

        // Holding Commit 확인
        Holding committedHolding = holdingRepository.findByAccountAndStock(savedAccount, savedStock).orElseThrow();
        assertThat(committedHolding.getQuantity()).isEqualTo(quantity);
        assertThat(committedHolding.getTotalPurchaseAmount()).isEqualByComparingTo(totalAmount);

        // Order Commit 확인
        List<Order> committedOrders = orderRepository.findAll();
        assertThat(committedOrders).hasSize(1);

        Order committedOrder  = committedOrders.get(0);
        assertThat(committedOrder .getQuantity()).isEqualTo(quantity);
        assertThat(committedOrder.getOrderPrice()).isEqualByComparingTo(currentPrice);
        assertThat(committedOrder.getTotalAmount()).isEqualByComparingTo(totalAmount);

        // TransactionHistory Commit 확인
        List<TransactionHistory> committedHistories = transactionHistoryRepository.findAll();
        assertThat(committedHistories).hasSize(1);

        TransactionHistory committedHistory = committedHistories.get(0);

        assertThat(committedHistory.getAmount()).isEqualByComparingTo(totalAmount);
        assertThat(committedHistory.getBeforeBalance()).isEqualByComparingTo(SEED_MONEY);
        assertThat(committedHistory.getAfterBalance()).isEqualByComparingTo(expectedBalance);

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
