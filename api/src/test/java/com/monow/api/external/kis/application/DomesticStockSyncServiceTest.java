package com.monow.api.external.kis.application;


import com.monow.api.external.kis.stockmaster.DomesticStockMarketType;
import com.monow.api.external.kis.stockmaster.KisStockMasterDownloader;
import com.monow.api.external.kis.stockmaster.KisStockMasterExtractor;
import com.monow.api.external.kis.stockmaster.KisStockMasterParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DomesticStockSyncServiceTest {

    @Mock
    private KisStockMasterDownloader kisStockMasterDownloader;

    @Mock
    private KisStockMasterExtractor kisStockMasterExtractor;

    @Mock
    private KisStockMasterParser kisStockMasterParser;

    @Mock
    private StockInfoSyncService stockInfoSyncService;

    @InjectMocks
    private DomesticStockSyncService domesticStockSyncService;


    @Nested
    @DisplayName("국내주식 종목 정보 동기화")
    class SyncDomesticStocks {

        @Test
        @DisplayName("[성공] - 추출한 stockCode 중복 제거 후 동기화")
        void syncDomesticStocks_whenStockCodesParsed_syncDistinctStockCodes() {
            // Given
            Map<DomesticStockMarketType, byte[]> zipFiles =
                    new EnumMap<>(DomesticStockMarketType.class);
            zipFiles.put(DomesticStockMarketType.KOSPI, new byte[]{1});

            Map<DomesticStockMarketType, byte[]> mstFiles =
                    new EnumMap<>(DomesticStockMarketType.class);
            mstFiles.put(DomesticStockMarketType.KOSPI, new byte[]{2});

            Map<DomesticStockMarketType, List<String>> stockCodes =
                    new EnumMap<>(DomesticStockMarketType.class);

            stockCodes.put(DomesticStockMarketType.KOSPI, List.of("005930", "000660"));
            stockCodes.put(DomesticStockMarketType.KOSDAQ, List.of("035720"));
            stockCodes.put(DomesticStockMarketType.NXT_KOSPI, List.of("005930"));
            stockCodes.put(DomesticStockMarketType.NXT_KOSDAQ, List.of("035720"));


            given(kisStockMasterDownloader.downloaderDomesticStock())
                    .willReturn(zipFiles);

            given(kisStockMasterExtractor.extractMstFiles(zipFiles))
                    .willReturn(mstFiles);

            given(kisStockMasterParser.parseStockCodes(mstFiles))
                    .willReturn(stockCodes);


            // When
            domesticStockSyncService.syncDomesticStocks();

            // Then
            verify(kisStockMasterDownloader).downloaderDomesticStock();
            verify(kisStockMasterExtractor).extractMstFiles(zipFiles);
            verify(kisStockMasterParser).parseStockCodes(mstFiles);

            verify(stockInfoSyncService).syncStockInfo("005930");
            verify(stockInfoSyncService).syncStockInfo("000660");
            verify(stockInfoSyncService).syncStockInfo("035720");
            verify(stockInfoSyncService, times(3)).syncStockInfo(any());


        }
    }

}
