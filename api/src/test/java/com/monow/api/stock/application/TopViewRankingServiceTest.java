package com.monow.api.stock.application;

import com.monow.api.external.kis.client.KisTopViewClient;
import com.monow.api.external.kis.dto.response.KisTopViewItem;

import com.monow.api.stock.dto.response.TopViewRankingResponse;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TopViewRankingServiceTest {

    @Mock
    private KisTopViewClient kisTopViewClient;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private TopViewRankingService topViewRankingService;




    @Nested
    @DisplayName("인기 종목 랭킹 조회")
    class GetTopViewRankStocks {

        @Test
        @DisplayName("[성공] - KIS 조회상위 응답에서 TOP 10개 종목 반환")
        void getTopViewRankStocks_whenKisApiReturnsRanks_returnsTop20Stocks() {
            // Given
            int limit = 10;
            List<KisTopViewItem> kisItems = createKisTopViewItems(25);
            List<Stock> stocks = createStocks(25);

            String firstStockCode = "005930";
            String firstStockName = "삼성전자";

            given(kisTopViewClient.fetchTopViewStocks())
                    .willReturn(kisItems);


            given(stockRepository.findByStockCodeIn(anyList()))
                    .willReturn(stocks);


            // When
            TopViewRankingResponse result = topViewRankingService.getTopViewRankStocks(limit);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.rankingType()).isEqualTo("TOP_VIEW");
            assertThat(result.items()).hasSize(20);
            assertThat(result.items().get(0).rank()).isEqualTo(1);
            assertThat(result.items().get(9).rank()).isEqualTo(10);
            assertThat(result.items().get(0).stockCode()).isEqualTo(firstStockCode);
            assertThat(result.items().get(0).stockName()).isEqualTo(firstStockName);

            then(kisTopViewClient)
                    .should(times(1))
                    .fetchTopViewStocks();

            then(stockRepository)
                    .should(times(1))
                    .findByStockCodeIn(anyList());


            log.info("result = {}", result);
            log.info("items size = {}", result.items().size());
            for (int i = 0; i < 10; i++) {
                log.info("{}th item = {}", i + 1, result.items().get(i));
            }

        }


        private List<Stock> createStocks(int count) {
            List<Stock> stocks = new ArrayList<>();

            stocks.add(createStock("005930", "삼성전자"));

            for (int i = 2; i <= count; i++) {
                String stockCode = String.format("%06d", i);
                String stockName = "테스트종목" + i;

                stocks.add(createStock(stockCode, stockName));
            }

            return stocks;
        }

        private List<KisTopViewItem> createKisTopViewItems(int count) {
            List<KisTopViewItem> items = new ArrayList<>();

            items.add(createKisTopViewItem("J", "005930"));

            for (int i = 2; i <= count; i++) {
                String stockCode = String.format("%06d",i);
                items.add(createKisTopViewItem("J", stockCode));

            }

            return items;

        }


        private KisTopViewItem createKisTopViewItem(String marketCode, String stockCode) {
            return new KisTopViewItem(marketCode, stockCode);
        }
    }
    private Stock createStock(String stockCode, String stockName) {
        return Stock.createStock(
                "TEST_PRODUCT_NUMBER",
                "TEST_STANDARD_PRODUCT_NUMBER",
                stockCode,
                stockName,
                stockName,
                "KOSPI",
                "TEST_PRODUCT_TYPE_CODE",
                "TEST_PRODUCT_CLASS_CODE",
                "TEST_PRODUCT_CLASS_NAME",
                "TEST_INVESTMENT_PRODUCT_TYPE_CODE",
                "TEST_INVESTMENT_PRODUCT_TYPE_NAME"
        );
    }

}