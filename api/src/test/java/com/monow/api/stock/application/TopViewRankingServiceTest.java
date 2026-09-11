package com.monow.api.stock.application;

import com.monow.api.external.kis.client.KisTopViewClient;
import com.monow.api.external.kis.dto.response.KisTopViewItem;
import com.monow.api.stock.ranking.application.TopViewRankingService;
import com.monow.api.stock.ranking.dto.response.TopViewRankingResponse;
import com.monow.domain.stock.entity.DomesticStockMarketType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TopViewRankingServiceTest {

    private static final String ETF_PRODUCT_CLASS_CODE = "101018";
    private static final String STOCK_CODE = "005930";

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
        void getTopViewRankStocks_whenKisApiReturnsRanks_returnsTop10Stocks() {
            // Given
            int limit = 10;
            List<KisTopViewItem> kisItems = createKisTopViewItems(25);
            List<Stock> stocks = createStocks(25);

            String firstStockName = "삼성전자";

            given(kisTopViewClient.fetchTopViewStocks())
                    .willReturn(kisItems);

            given(stockRepository.findByStockCodeInAndProductClassCodeNot(
                    anyList(),
                    eq(ETF_PRODUCT_CLASS_CODE)
            )).willReturn(stocks);

            // When
            TopViewRankingResponse result = topViewRankingService.getTopViewRankStocks(limit);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.rankingType()).isEqualTo("TOP_VIEW");

            assertThat(result.items()).hasSize(10);

            assertThat(result.items().get(0).rank()).isEqualTo(1);
            assertThat(result.items().get(9).rank()).isEqualTo(10);
            assertThat(result.items().get(0).stockCode()).isEqualTo(STOCK_CODE);
            assertThat(result.items().get(0).stockName()).isEqualTo(firstStockName);

            then(kisTopViewClient)
                    .should(times(1))
                    .fetchTopViewStocks();

            then(stockRepository)
                    .should(times(1))
                    .findByStockCodeInAndProductClassCodeNot(
                            anyList(),
                            eq(ETF_PRODUCT_CLASS_CODE)
                    );

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
                String stockCode = String.format("%06d", i);
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