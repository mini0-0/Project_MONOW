package com.monow.api.external.kis.application;

import com.monow.api.external.kis.stockmaster.KisStockMasterDownloader;
import com.monow.api.external.kis.stockmaster.KisStockMasterExtractor;
import com.monow.api.external.kis.stockmaster.KisStockMasterParser;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    /**
     * 특정 종목 하나 수동 동기화
     */
    public void syncDomesticStock(String stockCode) {
        Map<DomesticStockMarketType, List<String>> stockCodesByMarket = loadStockCodesByMarket();

        Set<String> kospiCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.KOSPI
                );

        Set<String> kosdaqCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.KOSDAQ
                );

        Set<String> nxtKospiCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.NXT_KOSPI
                );

        Set<String> nxtKosdaqCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.NXT_KOSDAQ
                );

        if (stockRepository.existsByStockCode(stockCode)) {
            return;
        }

        syncStock(
                stockCode,
                kospiCodes,
                kosdaqCodes,
                nxtKospiCodes,
                nxtKosdaqCodes
        );
    }

    /**
     * 요청받은 여러 종목 수동 동기화
     */
    public void syncSelectedDomesticStocks(List<String> stockCodes) {
        Map<DomesticStockMarketType, List<String>> stockCodesByMarket = loadStockCodesByMarket();

        Set<String> kospiCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.KOSPI
                );

        Set<String> kosdaqCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.KOSDAQ
                );

        Set<String> nxtKospiCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.NXT_KOSPI
                );

        Set<String> nxtKosdaqCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.NXT_KOSDAQ
                );

        List<String> distinctStockCodes =
                stockCodes.stream()
                        .distinct()
                        .toList();

        Set<String> existingStockCodes =
                findExistingStockCodes(distinctStockCodes);

        for (String stockCode : distinctStockCodes) {
            if (existingStockCodes.contains(stockCode)) {
                continue;
            }

            syncStock(
                    stockCode,
                    kospiCodes,
                    kosdaqCodes,
                    nxtKospiCodes,
                    nxtKosdaqCodes
            );

            sleep(100);
        }
    }

    /**
     * KIS 마스터 기준 전체 국내 종목 동기화
     */
    public void syncDomesticStocks() {
        Map<DomesticStockMarketType, List<String>> stockCodesByMarket = loadStockCodesByMarket();

        Set<String> kospiCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.KOSPI
                );

        Set<String> kosdaqCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.KOSDAQ
                );

        Set<String> nxtKospiCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.NXT_KOSPI
                );

        Set<String> nxtKosdaqCodes = getStockCodes(
                        stockCodesByMarket,
                        DomesticStockMarketType.NXT_KOSDAQ
                );
        List<String> allStockCodes = new ArrayList<>();

        allStockCodes.addAll(kospiCodes);
        allStockCodes.addAll(kosdaqCodes);

        Set<String> existingStockCodes =
                findExistingStockCodes(allStockCodes);

        for (String stockCode : allStockCodes) {
            if (existingStockCodes.contains(stockCode)) {
                continue;
            }

            syncStock(
                    stockCode,
                    kospiCodes,
                    kosdaqCodes,
                    nxtKospiCodes,
                    nxtKosdaqCodes
            );

            sleep(100);
        }
    }

    /**
     * 종목 하나의 시장 및 거래 가능 여부 판단
     */
    private void syncStock(
            String stockCode,
            Set<String> kospiCodes,
            Set<String> kosdaqCodes,
            Set<String> nxtKospiCodes,
            Set<String> nxtKosdaqCodes
    ) {
        DomesticStockMarketType marketType;

        if (kospiCodes.contains(stockCode)) {
            marketType = DomesticStockMarketType.KOSPI;
        } else if (kosdaqCodes.contains(stockCode)) {
            marketType = DomesticStockMarketType.KOSDAQ;
        } else {
            return;
        }

        boolean krxTradable = kospiCodes.contains(stockCode) || kosdaqCodes.contains(stockCode);

        boolean nxtTradable = nxtKospiCodes.contains(stockCode) || nxtKosdaqCodes.contains(stockCode);

        stockInfoSyncService.syncStockInfo(
                stockCode,
                marketType,
                krxTradable,
                nxtTradable
        );
    }

    /**
     * DB에 이미 저장되어 있는 Stock의 stockCode 조회
     */
    private Set<String> findExistingStockCodes(
            List<String> stockCodes
    ) {
        if (stockCodes.isEmpty()) {
            return Set.of();
        }

        return stockRepository.findByStockCodeIn(stockCodes)
                .stream()
                .map(Stock::getStockCode)
                .collect(Collectors.toSet());
    }

    /**
     * KIS 마스터 다운로드
     * → 압축 해제
     * → 시장별 stockCode 파싱
     */
    private Map<DomesticStockMarketType, List<String>> loadStockCodesByMarket() {
        Map<DomesticStockMarketType, byte[]> zipFiles = kisStockMasterDownloader.downloaderDomesticStock();

        Map<DomesticStockMarketType, byte[]> mstFiles = kisStockMasterExtractor.extractMstFiles(zipFiles);

        return kisStockMasterParser.parseStockCodes(mstFiles);
    }

    /**
     * 특정 시장의 종목코드 목록을 Set으로 변환
     */
    private Set<String> getStockCodes(
            Map<DomesticStockMarketType, List<String>> stockCodesByMarket,
            DomesticStockMarketType marketType
    ) {
        return Set.copyOf(
                stockCodesByMarket.getOrDefault(
                        marketType,
                        List.of()
                )
        );
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "국내 종목 동기화 작업이 중단되었습니다.",
                    e
            );
        }
    }
}