package com.monow.domain.stock.repository;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.domain.stock.repository.StockSearchQueryResult;
import com.monow.global.config.QueryDSLConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDSLConfig.class)
class StockRepositoryImplTest {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private StockRepository stockRepository;

    @Nested
    @DisplayName("종목 검색")
    class SearchByKeyword {

        @Test
        @DisplayName("[성공] - 종목명이 검색어로 시작하는 종목을 조회한다")
        void givenStockNamePrefix_whenSearchByKeyword_thenReturnMatchedStocks() {
            // Given
            String keyword = "삼";

            Stock samsungElectronics = createStock("005930", "삼성전자");

            Stock samsungElectroMechanics = createStock("009150", "삼성전기");

            Stock containsOnlyStock = createStock("123456", "한화삼성");

            stockRepository.saveAll(
                    List.of(
                            samsungElectronics,
                            samsungElectroMechanics,
                            containsOnlyStock
                    )
            );

            stockRepository.flush();


            // When
            Page<StockSearchQueryResult> results = stockRepository.searchByKeyword(keyword, pageable);

            // Then
            assertThat(results).hasSize(2);

            assertThat(results)
                    .extracting(StockSearchQueryResult::stockName)
                    .contains("삼성전자", "삼성전기")
                    .doesNotContain("한화삼성");

        }

        @Test
        @DisplayName("[성공] - 종목코드가 검색어로 시작하는 종목을 조회한다")
        void givenStockCodePrefix_whenSearchByKeyword_thenReturnMatchedStocks() {
            // Given
            String keyword = "00";

            Stock samsungElectronics = createStock("005930", "삼성전자");

            Stock samsungElectroMechanics = createStock("009150", "삼성전기");

            Stock kbFinancial = createStock("105560", "KB금융");

            stockRepository.saveAll(
                    List.of(
                            samsungElectronics,
                            samsungElectroMechanics,
                            kbFinancial
                    )
            );

            stockRepository.flush();

            // When
            Page<StockSearchQueryResult> results = stockRepository.searchByKeyword(keyword, pageable);

            // Then
            assertThat(results).hasSize(2);

            assertThat(results)
                    .extracting(StockSearchQueryResult::stockCode)
                    .contains("005930", "009150")
                    .doesNotContain("105560");


        }

        @Test
        @DisplayName("[성공] - 정확히 일치하는 종목을 우선 반환한다")
        void givenExactMatch_whenSearchByKeyword_thenReturnExactMatchFirst() {
            // Given
            String keyword = "삼성전자";

            Stock samsungElectronics = createStock("005930", "삼성전자");

            Stock samsungElectronicsPreferred = createStock("005935", "삼성전자우");

            stockRepository.saveAll(
                    List.of(
                            samsungElectronicsPreferred,
                            samsungElectronics
                    )
            );

            stockRepository.flush();

            // When
            Page<StockSearchQueryResult> results = stockRepository.searchByKeyword(keyword, pageable);

            // Then
            assertThat(results).hasSize(2);

            assertThat(results.getContent().get(0).stockName())
                    .isEqualTo("삼성전자");

            assertThat(results.getContent().get(1).stockName())
                    .isEqualTo("삼성전자우");
        }

        @Test
        @DisplayName("[성공] - 동일한 검색 우선순위의 종목은 종목명 순으로 반환한다")
        void givenSamePriorityStocks_whenSearchByKeyword_thenReturnStockNameOrder() {
            // Given
            String keyword = "삼성";

            Stock stockC = createStock("900003", "삼성C");

            Stock stockA = createStock("900001", "삼성A");

            Stock stockB = createStock("900002", "삼성B");

            stockRepository.saveAll(
                    List.of(
                            stockC,
                            stockA,
                            stockB
                    )
            );

            stockRepository.flush();

            // When
            Page<StockSearchQueryResult> results = stockRepository.searchByKeyword(keyword, pageable);

            // Then
            assertThat(results)
                    .extracting(StockSearchQueryResult::stockName)
                    .containsExactly(
                            "삼성A",
                            "삼성B",
                            "삼성C"
                    );


        }

        @Test
        @DisplayName("[성공] - 종목 검색 결과를 최대 10개까지 반환한다")
        void givenMoreThanTenMatchedStocks_whenSearchByKeyword_thenReturnMaximumTenStocks() {
            // Given
            String keyword = "테스트";

            for (int i =1 ; i <= 11; i++) {
                Stock stock = createStock(
                        String.format("9%05d", i),
                        String.format("테스트종목%02d", i)
                );

                stockRepository.save(stock);
            }

            stockRepository.flush();

            // When
            Page<StockSearchQueryResult> results = stockRepository.searchByKeyword(keyword, pageable);

            // Then
            assertThat(results).hasSize(10);


        }

        @Test
        @DisplayName("[성공] - 주권이 아닌 종목은 검색 결과에서 제외한다")
        void givenNonEquityStock_whenSearchByKeyword_thenExcludeNonEquityStock() {
            // Given
            String keyword = "삼성";

            Stock equityStock = createStock("005930", "삼성전자");

            Stock etfStock = Stock.createStock(
                    "TEST_PRODUCT_123456",
                    "TEST_STANDARD_123456",
                    "123456",
                    "삼성ETF",
                    "삼성ETF",
                    DomesticStockMarketType.KOSPI,
                    "300",
                    "101010",
                    "ETF",
                    "1010",
                    "주식",
                    true,
                    true
            );

            stockRepository.saveAll(
                    List.of(equityStock, etfStock)
            );

            stockRepository.flush();

            // When
            Page<StockSearchQueryResult> results = stockRepository.searchByKeyword(keyword, pageable);

            // Then
            assertThat(results)
                    .extracting(StockSearchQueryResult::stockName)
                    .contains("삼성전자");

            assertThat(results)
                    .extracting(StockSearchQueryResult::stockName)
                    .doesNotContain("삼성ETF");

        }

    }

    private Stock createStock(
            String stockCode,
            String stockName
    ) {
        return Stock.createStock(
                "TEST_PRODUCT_" + stockCode,
                "TEST_STANDARD_" + stockCode,
                stockCode,
                stockName + "보통주",
                stockName,
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