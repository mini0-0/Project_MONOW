package com.monow.api.stock.search.application;

import com.monow.api.stock.search.dto.response.StockSearchResponse;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.repository.StockSearchQueryResult;
import com.monow.domain.stock.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockSearchQueryServiceTest {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockSearchQueryService stockSearchQueryService;

    @Nested
    @DisplayName("종목 검색")
    class SearchStocks {

        @Test
        @DisplayName("[성공] - 검색된 종목 목록을 응답으로 반환한다")
        void givenSearchResults_whenSearchStocks_thenReturnStockResponses() {
            // Given
            String keyword = "삼성";

            String firstStockCode = "005930";
            String firstStockName = "삼성전자";
            String secondStockCode = "009150";
            String secondStockName = "삼성전기";

            String productClassName = "주권";
            DomesticStockMarketType marketType = DomesticStockMarketType.KOSPI;

            StockSearchQueryResult firstStock = new StockSearchQueryResult(
                    firstStockCode,
                    firstStockName,
                    productClassName,
                    marketType,
                    true
            );

            StockSearchQueryResult secondStock = new StockSearchQueryResult(
                    secondStockCode,
                    secondStockName,
                    productClassName,
                    marketType,
                    true
            );

            List<StockSearchQueryResult> searchResults = List.of(firstStock, secondStock);

            Page<StockSearchQueryResult> searchResultPage =
                    new PageImpl<>(
                        searchResults,
                        pageable,
                        searchResults.size()
            );

            given(stockRepository.searchByKeyword(keyword, pageable))
                    .willReturn(searchResultPage);

            // When
            Page<StockSearchResponse> results = stockSearchQueryService.searchStocks(keyword, pageable);

            // Then
            assertThat(results).hasSize(2);

            assertThat(results.getContent().get(0).stockCode()).isEqualTo(firstStockCode);
            assertThat(results.getContent().get(0).stockName()).isEqualTo(firstStockName);
            assertThat(results.getContent().get(0).marketType()).isEqualTo(marketType);
            assertThat(results.getContent().get(1).stockCode()).isEqualTo(secondStockCode);

            verify(stockRepository).searchByKeyword(keyword, pageable);
        }

        @Test
        @DisplayName("[성공] - 검색 결과가 없으면 빈 목록을 반환한다")
        void givenNoSearchResults_whenSearchStocks_thenReturnEmptyList() {
            // Given
            String keyword = "없는종목";

            Page<StockSearchQueryResult> emptyPage =
                    new PageImpl<>(
                            List.of(),
                            pageable,
                            0
                    );

            given(stockRepository.searchByKeyword(keyword, pageable))
                    .willReturn(emptyPage);

            // When
            Page<StockSearchResponse> results = stockSearchQueryService.searchStocks(keyword, pageable);

            // Then
            assertThat(results).isEmpty();

            verify(stockRepository).searchByKeyword(keyword, pageable);
        }
    }
}