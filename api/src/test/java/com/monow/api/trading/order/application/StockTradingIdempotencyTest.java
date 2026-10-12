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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class StockTradingIdempotencyTest {

    private static final String ACCOUNT_NUMBER = "MONOW260101123456";
    private static final String STOCK_CODE = "005930";

    private static final CurrentPriceMarketType MARKET_TYPE = CurrentPriceMarketType.KRX;

    private static final String IDEMPOTENCY_KEY = "order-request-001";

    private static final String ANOTHER_IDEMPOTENCY_KEY = "order-request-002";

    @Autowired
    private StockTradingTransactionService stockTradingTransactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private HoldingRepository holdingRepository;

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
    @DisplayName("[성공] - 동일 멱등성 키로 매수 요청을 재시도해도 주문은 한 번만 처리된다")
    void GivenSameIdempotencyKey_WhenBuyStockTwice_ThenProcessOnlyOnce() {
        // Given
        BigDecimal initialBalance = BigDecimal.valueOf(2_000_000);
        BigDecimal currentPrice = BigDecimal.valueOf(700_000);

        int buyQuantity = 1;

        User savedUser = userRepository.save(createUser());
        Account savedAccount = accountRepository.save(createAccount(savedUser, initialBalance));
        Stock savedStock = stockRepository.save(createStock());

        StockOrderPrice orderPrice = new StockOrderPrice(MARKET_TYPE, currentPrice);

        // When
        stockTradingTransactionService.buyStock(
                savedUser.getId(),
                STOCK_CODE,
                buyQuantity,
                orderPrice,
                IDEMPOTENCY_KEY
        );

        stockTradingTransactionService.buyStock(
                savedUser.getId(),
                STOCK_CODE,
                buyQuantity,
                orderPrice,
                IDEMPOTENCY_KEY
        );


        // Then
        Account resultAccount = accountRepository.findByUserId(savedUser.getId())
                .orElseThrow();
        Stock resultStock = stockRepository.findByStockCode(STOCK_CODE)
                .orElseThrow();
        Holding resultHolding = holdingRepository.findByAccountAndStock(resultAccount, resultStock)
                .orElseThrow();


        long orderCount = orderRepository.count();
        long transactionHistoryCount = transactionHistoryRepository.count();

        assertThat(resultAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1_300_000));
        assertThat(resultHolding.getQuantity()).isEqualTo(1);
        assertThat(orderCount).isEqualTo(1);
        assertThat(transactionHistoryCount).isEqualTo(1);


    }


    @Test
    @DisplayName("[성공] - 멱등성 키가 다르면 동일한 매수 요청도 각각 처리된다")
    void GivenDifferentIdempotencyKeys_WhenBuyStockTwice_ThenProcessBothOrders() {
        // Given
        BigDecimal initialBalance = BigDecimal.valueOf(2_000_000);
        BigDecimal currentPrice = BigDecimal.valueOf(700_000);

        int buyQuantity = 1;

        User savedUser = userRepository.save(createUser());
        Account savedAccount = accountRepository.save(createAccount(savedUser, initialBalance));
        Stock savedStock = stockRepository.save(createStock());

        StockOrderPrice orderPrice = new StockOrderPrice(MARKET_TYPE, currentPrice);

        // When
        stockTradingTransactionService.buyStock(
                savedUser.getId(),
                STOCK_CODE,
                buyQuantity,
                orderPrice,
                IDEMPOTENCY_KEY
        );

        stockTradingTransactionService.buyStock(
                savedUser.getId(),
                STOCK_CODE,
                buyQuantity,
                orderPrice,
                ANOTHER_IDEMPOTENCY_KEY
        );



        // Then
        Account resultAccount = accountRepository.findByUserId(savedUser.getId())
                .orElseThrow();
        Stock resultStock = stockRepository.findByStockCode(STOCK_CODE)
                .orElseThrow();
        Holding resultHolding = holdingRepository.findByAccountAndStock(resultAccount, resultStock)
                .orElseThrow();


        long orderCount = orderRepository.count();
        long transactionHistoryCount = transactionHistoryRepository.count();

        assertThat(resultAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600_000));
        assertThat(resultHolding.getQuantity()).isEqualTo(2);
        assertThat(orderCount).isEqualTo(2);
        assertThat(transactionHistoryCount).isEqualTo(2);



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
