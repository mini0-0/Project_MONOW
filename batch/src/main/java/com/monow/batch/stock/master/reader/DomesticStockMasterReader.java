package com.monow.batch.stock.master.reader;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.batch.stock.master.dto.DomesticStockMasterItem;
import com.monow.external.kis.stock.stockmaster.KisStockMasterDownloader;
import com.monow.external.kis.stock.stockmaster.KisStockMasterExtractor;
import com.monow.external.kis.stock.stockmaster.KisStockMasterParser;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@StepScope
@RequiredArgsConstructor
public class DomesticStockMasterReader implements ItemReader<DomesticStockMasterItem> {

    private final KisStockMasterDownloader kisStockMasterDownloader;

    private final KisStockMasterExtractor kisStockMasterExtractor;

    private final KisStockMasterParser kisStockMasterParser;

    private List<DomesticStockMasterItem> items;

    private int currentIndex = 0;

    @Override
    public DomesticStockMasterItem read() {
        if (items == null) {
            items = loadDomesticStockMasterItems();
        }

        if (currentIndex >= items.size()) {
            return null;
        }

        return items.get(currentIndex++);
    }

    private List<DomesticStockMasterItem> loadDomesticStockMasterItems() {
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

        Set<String> allStockCodes = new LinkedHashSet<>();

        allStockCodes.addAll(kospiCodes);
        allStockCodes.addAll(kosdaqCodes);

        List<DomesticStockMasterItem> result = new ArrayList<>();

        for (String stockCode : allStockCodes) {
            DomesticStockMarketType marketType = resolveMarketType(stockCode, kospiCodes, kosdaqCodes);

            boolean krxTradable = kospiCodes.contains(stockCode) || kosdaqCodes.contains(stockCode);
            boolean nxtTradable = nxtKospiCodes.contains(stockCode) || nxtKosdaqCodes.contains(stockCode);

            result.add(new DomesticStockMasterItem(stockCode, marketType, krxTradable, nxtTradable));
        }

        return result;

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

    private DomesticStockMarketType resolveMarketType(
            String stockCode,
            Set<String> kospiCodes,
            Set<String> kosdaqCodes
    ) {
        if (kospiCodes.contains(stockCode)) {
            return DomesticStockMarketType.KOSPI;
        }

        if (kosdaqCodes.contains(stockCode)) {
            return DomesticStockMarketType.KOSDAQ;
        }

        throw new IllegalArgumentException(
                "지원하지 않는 국내 종목 코드입니다. stockCode=" + stockCode
        );
    }

}
