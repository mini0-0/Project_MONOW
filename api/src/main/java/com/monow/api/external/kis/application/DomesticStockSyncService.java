package com.monow.api.external.kis.application;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.api.external.kis.stockmaster.KisStockMasterDownloader;
import com.monow.api.external.kis.stockmaster.KisStockMasterExtractor;
import com.monow.api.external.kis.stockmaster.KisStockMasterParser;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DomesticStockSyncService {

    private final KisStockMasterDownloader kisStockMasterDownloader;

    private final KisStockMasterExtractor kisStockMasterExtractor;

    private final KisStockMasterParser kisStockMasterParser;

    private final StockInfoSyncService stockInfoSyncService;

    private final StockRepository stockRepository;

    public void syncDomesticStocks() {
        Map<DomesticStockMarketType, byte[]> zipFiles = kisStockMasterDownloader.downloaderDomesticStock();

        Map<DomesticStockMarketType, byte[]> mstFiles = kisStockMasterExtractor.extractMstFiles(zipFiles);

        Map<DomesticStockMarketType, List<String>> stockCodesByMarket = kisStockMasterParser.parseStockCodes(mstFiles);

        List<String> allStockCodes = stockCodesByMarket.values()
                .stream()
                .flatMap(List::stream)
                .distinct()
                .toList();

        Set<String> existingStockCodes =
                stockRepository.findByStockCodeIn(allStockCodes)
                        .stream()
                        .map(Stock::getStockCode)
                        .collect(Collectors.toSet());

        for (Map.Entry<DomesticStockMarketType, List<String>> entry
                : stockCodesByMarket.entrySet()) {

            DomesticStockMarketType marketType = entry.getKey();
            List<String> stockCodes = entry.getValue();

            for (String stockCode : stockCodes) {
                if (existingStockCodes.contains(stockCode)) {
                    continue;
                }

                stockInfoSyncService.syncStockInfo(
                        stockCode,
                        marketType
                );

                sleep(100);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("국내 종목 동기화 작업이 중단되었습니다.", e);
        }
    }
}
