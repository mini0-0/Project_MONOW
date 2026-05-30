package com.monow.api.external.kis.application;

import com.monow.api.external.kis.stockmaster.DomesticStockMarketType;
import com.monow.api.external.kis.stockmaster.KisStockMasterDownloader;
import com.monow.api.external.kis.stockmaster.KisStockMasterExtractor;
import com.monow.api.external.kis.stockmaster.KisStockMasterParser;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

@Service
@RequiredArgsConstructor
public class DomesticStockSyncService {

    private final KisStockMasterDownloader kisStockMasterDownloader;

    private final KisStockMasterExtractor kisStockMasterExtractor;

    private final KisStockMasterParser kisStockMasterParser;

    private final StockInfoSyncService stockInfoSyncService;


    @Transactional
    public void syncDomesticStocks() {
        Map<DomesticStockMarketType, byte[]> zipFiles =
                kisStockMasterDownloader.downloaderDomesticStock();

        Map<DomesticStockMarketType, byte[]> mstFiles =
                kisStockMasterExtractor.extractMstFiles(zipFiles);

        Map<DomesticStockMarketType, List<String>> stockCodes =
                kisStockMasterParser.parseStockCodes(mstFiles);

        List<String> distinctStockCodes = stockCodes.values()
                .stream()
                .flatMap(List::stream)
                .distinct()
                .toList();

        for (String stockCode : distinctStockCodes) {
            stockInfoSyncService.syncStockInfo(stockCode);
            sleep(50);
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
