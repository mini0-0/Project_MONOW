package com.monow.api.portfolio.application;

import com.monow.api.external.kis.type.CurrentPriceMarketType;
import com.monow.api.portfolio.dto.response.PortfolioHoldingResponse;
import com.monow.api.portfolio.dto.response.PortfolioResponse;
import com.monow.api.portfolio.dto.response.PortfolioSummaryResponse;
import com.monow.api.stock.application.currentprice.StockCurrentPriceMarketSelection;
import com.monow.api.stock.application.currentprice.StockCurrentPriceMarketSelector;
import com.monow.api.stock.application.currentprice.StockMarketStatus;
import com.monow.api.stock.application.realtime.StockRealtimePriceCacheService;
import com.monow.api.stock.dto.response.StockRealtimePriceResponse;
import com.monow.domain.account.entity.Account;
import com.monow.domain.account.repository.AccountRepository;
import com.monow.domain.holding.entity.Holding;
import com.monow.domain.holding.repository.HoldingRepository;
import com.monow.domain.holding.repository.PortfolioHoldingQueryResult;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioQueryServiceTest {

    private static final Long USER_ID = 2L;
    private static final String ACCOUNT_NUMBER = "MONOW260101123456";
    private static final CurrentPriceMarketType MARKET_TYPE = CurrentPriceMarketType.INTEGRATED;
    private static final String STOCK_CODE_SAMSUNG = "005930";
    private static final String STOCK_CODE_SK = "000660";

    private static final BigDecimal SAMSUNG_CURRENT_PRICE = BigDecimal.valueOf(380_000);
    private static final BigDecimal SK_CURRENT_PRICE = BigDecimal.valueOf(1_600_000);
    private static final BigDecimal SEED_MONEY = BigDecimal.valueOf(100_000_000L);

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private StockRealtimePriceCacheService stockRealtimePriceCacheService;

    @Mock
    private StockCurrentPriceMarketSelector stockCurrentPriceMarketSelector;

    @InjectMocks
    private PortfolioQueryService portfolioQueryService;

    @Nested
    @DisplayName("포트폴리오 전체 자산 요약 조회")
    class GetPortfolioSummary {

        @Test
        @DisplayName("[성공] - 현금과 보유 종목이 존재할 때 선택된 시장 현재가로 전체 자산 요약 조회")
        void givenAccountHoldingsAndCurrentPrices_whenGetPortfolioSummary_thenReturnCalculatedSummary() {
            // Given
            User user = createUser();
            Account account = createAccount(user, SEED_MONEY);

            Stock samsungStock = createSamsungStock();
            Stock skStock = createSKStock();

            Holding samsungHolding = Holding.createHolding(
                    user,
                    account,
                    samsungStock,
                    100,
                    BigDecimal.valueOf(36_000_000)
            );

            Holding skHolding = Holding.createHolding(
                    user,
                    account,
                    skStock,
                    30,
                    BigDecimal.valueOf(45_000_000)
            );

            BigDecimal totalPurchaseAmount =
                    samsungHolding.getTotalPurchaseAmount()
                            .add(skHolding.getTotalPurchaseAmount());

            account.deductBalance(totalPurchaseAmount);

            StockRealtimePriceResponse samsungPriceResponse =
                    createSamsungRealtimePriceResponse(SAMSUNG_CURRENT_PRICE);

            StockRealtimePriceResponse skPriceResponse =
                    createSKRealtimePriceResponse(SK_CURRENT_PRICE);

            PortfolioHoldingQueryResult samsungHoldingQueryResult =
                    new PortfolioHoldingQueryResult(
                            STOCK_CODE_SAMSUNG,
                            "삼성전자",
                            DomesticStockMarketType.KOSPI,
                            100,
                            BigDecimal.valueOf(36_000_000)
                    );

            PortfolioHoldingQueryResult skHoldingQueryResult =
                    new PortfolioHoldingQueryResult(
                            STOCK_CODE_SK,
                            "SK하이닉스",
                            DomesticStockMarketType.KOSPI,
                            30,
                            BigDecimal.valueOf(45_000_000)
                    );

            StockCurrentPriceMarketSelection selection =
                    new StockCurrentPriceMarketSelection(
                            MARKET_TYPE,
                            StockMarketStatus.OPEN,
                            true
                    );

            given(accountRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(account));

            given(holdingRepository.findPortfolioHoldings(USER_ID))
                    .willReturn(List.of(
                            samsungHoldingQueryResult,
                            skHoldingQueryResult
                    ));

            given(stockCurrentPriceMarketSelector.select())
                    .willReturn(selection);

            given(stockRealtimePriceCacheService.findLatestPrices(
                    MARKET_TYPE,
                    List.of(STOCK_CODE_SAMSUNG, STOCK_CODE_SK)
            )).willReturn(Map.of(
                    STOCK_CODE_SAMSUNG, samsungPriceResponse,
                    STOCK_CODE_SK, skPriceResponse
            ));

            // 현재 보유 현금
            BigDecimal expectedCashBalance = BigDecimal.valueOf(19_000_000);

            // 총 투자 금액
            BigDecimal expectedTotalInvestment = BigDecimal.valueOf(81_000_000);

            // 주식 평가 금액
            BigDecimal expectedStockEvaluationAmount = BigDecimal.valueOf(86_000_000);

            // 평가 손익
            BigDecimal expectedProfitLoss = BigDecimal.valueOf(5_000_000);

            // 수익률
            BigDecimal expectedProfitRate = BigDecimal.valueOf(6.17);

            // 총 자산
            BigDecimal expectedTotalAsset = BigDecimal.valueOf(105_000_000);

            // When
            PortfolioResponse result = portfolioQueryService.getPortfolio(USER_ID);

            // Then
            PortfolioSummaryResponse summary = result.summary();

            assertThat(summary.cashBalance()).isEqualByComparingTo(expectedCashBalance);
            assertThat(summary.totalAsset()).isEqualByComparingTo(expectedTotalAsset);
            assertThat(summary.totalInvestment()).isEqualByComparingTo(expectedTotalInvestment);
            assertThat(summary.stockEvaluationAmount()).isEqualByComparingTo(expectedStockEvaluationAmount);
            assertThat(summary.profitLoss()).isEqualByComparingTo(expectedProfitLoss);
            assertThat(summary.profitRate()).isEqualByComparingTo(expectedProfitRate);

            assertThat(result.holdings()).hasSize(2);

            PortfolioHoldingResponse samsungHoldingResponse = result.holdings().get(0);

            assertThat(samsungHoldingResponse.stockCode()).isEqualTo(STOCK_CODE_SAMSUNG);
            assertThat(samsungHoldingResponse.stockName()).isEqualTo("삼성전자");
            assertThat(samsungHoldingResponse.quantity()).isEqualTo(100);
            assertThat(samsungHoldingResponse.totalPurchaseAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(36_000_000));
            assertThat(samsungHoldingResponse.averageBuyPrice())
                    .isEqualByComparingTo(BigDecimal.valueOf(360_000));
            assertThat(samsungHoldingResponse.currentPrice())
                    .isEqualByComparingTo(SAMSUNG_CURRENT_PRICE);
            assertThat(samsungHoldingResponse.evaluationAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(38_000_000));
            assertThat(samsungHoldingResponse.profitLoss())
                    .isEqualByComparingTo(BigDecimal.valueOf(2_000_000));
            assertThat(samsungHoldingResponse.profitRate())
                    .isEqualByComparingTo(BigDecimal.valueOf(5.56));

            verify(stockCurrentPriceMarketSelector).select();
            verify(stockRealtimePriceCacheService).findLatestPrices(
                    MARKET_TYPE,
                    List.of(STOCK_CODE_SAMSUNG, STOCK_CODE_SK)
            );
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
        return Account.createAccount(user, ACCOUNT_NUMBER, balance);
    }

    private Stock createSamsungStock() {
        return Stock.createStock(
                "00000A005930",
                "KR7005930003",
                STOCK_CODE_SAMSUNG,
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

    private Stock createSKStock() {
        return Stock.createStock(
                "00000A000660",
                "KR7000660001",
                STOCK_CODE_SK,
                "에스케이하이닉스보통주",
                "SK하이닉스",
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

    private StockRealtimePriceResponse createSamsungRealtimePriceResponse(
            BigDecimal currentPrice
    ) {
        BigDecimal changePrice = BigDecimal.valueOf(20_000);
        String changeSign = "2";
        BigDecimal changeRate = BigDecimal.valueOf(5.56);
        Long tradeVolume = 5_000_000L;
        BigDecimal tradeAmount = BigDecimal.valueOf(2_000_000_000_000L);
        BigDecimal openPrice = BigDecimal.valueOf(360_000);
        BigDecimal highPrice = BigDecimal.valueOf(390_000);
        BigDecimal lowPrice = BigDecimal.valueOf(355_000);
        LocalTime tradeTime = LocalTime.of(9, 0, 15);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 6, 9, 0, 16);

        return new StockRealtimePriceResponse(
                MARKET_TYPE,
                STOCK_CODE_SAMSUNG,
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

    private StockRealtimePriceResponse createSKRealtimePriceResponse(
            BigDecimal currentPrice
    ) {
        return new StockRealtimePriceResponse(
                MARKET_TYPE,
                STOCK_CODE_SK,
                currentPrice,
                BigDecimal.valueOf(50_000),
                "2",
                BigDecimal.valueOf(3.23),
                5_000_000L,
                BigDecimal.valueOf(8_000_000_000_000L),
                BigDecimal.valueOf(1_550_000),
                BigDecimal.valueOf(1_620_000),
                BigDecimal.valueOf(1_530_000),
                LocalTime.of(9, 0, 15),
                LocalDateTime.of(2026, 8, 6, 9, 0, 16)
        );
    }
}