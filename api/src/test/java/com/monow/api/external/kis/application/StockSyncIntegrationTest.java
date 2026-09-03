package com.monow.api.external.kis.application;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Transactional
public class StockSyncIntegrationTest {

    private static final String STOCK_CODE = "005930";
    private static final DomesticStockMarketType MARKET_TYPE = DomesticStockMarketType.KOSPI;
    private static final boolean KRX_TRADABLE = true;
    private static final boolean NXT_TRADABLE = true;

    @Autowired
    private StockInfoSyncService stockInfoSyncService;

    @Autowired
    private StockRepository stockRepository;

    @Nested
    @DisplayName("종목 정보 실제 저장")
    class StockInfo {

        @Test
        @DisplayName("[성공] - 한국투자 종목 API를 조회하여 stocks 테이블에 저장")
        void syncStockInfo_whenStockDoesNotExist_savesNewStock() {
            // Given

            // When
            stockInfoSyncService.syncStockInfo(STOCK_CODE, MARKET_TYPE, KRX_TRADABLE, NXT_TRADABLE);

            // Then
            List<Stock> savedStocks = stockRepository.findByStockCodeIn(List.of(STOCK_CODE));

            assertThat(savedStocks).hasSize(1);

            Stock stock = savedStocks.get(0);

            assertThat(stock.getStockCode()).isEqualTo(STOCK_CODE);
            assertThat(stock.getStockName()).isNotBlank();
            assertThat(stock.getProductNumber()).isNotBlank();
            assertThat(stock.getStandardProductNumber()).isNotBlank();
            assertThat(stock.getProductTypeCode()).isEqualTo("300");
            assertThat(stock.getMarketType()).isEqualTo(DomesticStockMarketType.KOSPI);
            assertThat(stock.getKrxTradable()).isTrue();
            assertThat(stock.getNxtTradable()).isFalse();
            assertThat(stock.getIsActive()).isTrue();

            log.info("saved stockCode={}, stockName={}, krxTradable={}, nxtTradable={}", stock.getStockCode(), stock.getStockName(), stock.getKrxTradable(), stock.getNxtTradable());

        }

        @Test
        @DisplayName("[스킵] - 이미 저장된 stocks는 중복 저장 하지 않음")
        void syncStockInfo_whenStockAlreadyExists_doesNotCreateDuplicate() {
            // Given
            stockInfoSyncService.syncStockInfo(STOCK_CODE, MARKET_TYPE, KRX_TRADABLE, NXT_TRADABLE);
            long beforeCount = stockRepository.countByStockCode(STOCK_CODE);

            // When
            stockInfoSyncService.syncStockInfo(STOCK_CODE, MARKET_TYPE, KRX_TRADABLE, NXT_TRADABLE);

            // Then
            long afterCount = stockRepository.countByStockCode(STOCK_CODE);
            assertThat(afterCount).isEqualTo(beforeCount);
            assertThat(afterCount).isEqualTo(1);

            log.info("beforeCount {}, afterCount{}", beforeCount, afterCount);

        }
    }

}
