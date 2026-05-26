package com.monow.api.external.kis.application;

import com.monow.api.external.kis.client.KisTokenClient;
import com.monow.api.external.kis.mapper.KisStockInfoMapper;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Transactional
public class StockSyncIntegrationTest {

    @Autowired
    private KisTokenClient kisTokenClient;

    @Autowired
    private KisStockInfoMapper kisStockInfoMapper;

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
            String stockCode = "005930";

            // When
            stockInfoSyncService.syncStockInfo(stockCode);

            // Then
            Optional<Stock> savedStock = stockRepository.findByStockCode(stockCode);

            assertThat(savedStock).isPresent();

            Stock stock = savedStock.get();

            assertThat(stock.getStockCode()).isEqualTo("005930");
            assertThat(stock.getStockName()).isNotBlank();
            assertThat(stock.getProductNumber()).isNotBlank();
            assertThat(stock.getStandardProductNumber()).isNotBlank();
            assertThat(stock.getProductTypeCode()).isEqualTo("300");
            assertThat(stock.getMarketType()).isEqualTo("DOMESTIC_STOCK");
            assertThat(stock.getIsActive()).isTrue();


        }

        @Test
        @DisplayName("[스킵] - 이미 저장된 stocks는 중복 저장 하지 않음")
        void syncStockInfo_whenStockAlreadyExists_doesNotCreateDuplicate() {
            // Given
            String stockCode = "005930";

            stockInfoSyncService.syncStockInfo(stockCode);
            long beforeCount = stockRepository.countByStockCode(stockCode);

            // When
            stockInfoSyncService.syncStockInfo(stockCode);

            // Then
            long afterCount = stockRepository.countByStockCode(stockCode);
            assertThat(afterCount).isEqualTo(beforeCount);
            assertThat(afterCount).isEqualTo(1);

            log.info("beforeCount {}, afterCount{}", beforeCount, afterCount);

        }
    }

}
