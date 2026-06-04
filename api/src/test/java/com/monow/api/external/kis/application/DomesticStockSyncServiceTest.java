package com.monow.api.external.kis.application;


import com.monow.api.external.kis.stockmaster.DomesticStockMarketType;
import com.monow.api.external.kis.stockmaster.KisStockMasterDownloader;
import com.monow.api.external.kis.stockmaster.KisStockMasterExtractor;
import com.monow.api.external.kis.stockmaster.KisStockMasterParser;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
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


import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Mock
    private StockRepository stockRepository;

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

            Stock existingStock = Stock.createStock(
                    "00000A005930",
                    "KR7005930003",
                    "005930",
                    "삼성전자보통주",
                    "삼성전자",
                    "DOMESTIC_STOCK",
                    "300",
                    "101010",
                    "주권",
                    "1010",
                    "주식"
            );


            given(kisStockMasterDownloader.downloaderDomesticStock())
                    .willReturn(zipFiles);

            given(kisStockMasterExtractor.extractMstFiles(zipFiles))
                    .willReturn(mstFiles);

            given(kisStockMasterParser.parseStockCodes(mstFiles))
                    .willReturn(stockCodes);

            // DB에 005930만 저장 되어 있다고 가정
            given(stockRepository.findByStockCodeIn(anyCollection()))
                    .willReturn(List.of(existingStock));



            // When
            domesticStockSyncService.syncDomesticStocks();

            // Then
            verify(kisStockMasterDownloader).downloaderDomesticStock();
            verify(kisStockMasterExtractor).extractMstFiles(zipFiles);
            verify(kisStockMasterParser).parseStockCodes(mstFiles);

            verify(stockInfoSyncService, never()).syncStockInfo("005930");

            verify(stockInfoSyncService).syncStockInfo("000660");
            verify(stockInfoSyncService).syncStockInfo("035720");

            verify(stockInfoSyncService, times(3)).syncStockInfo(any());


        }
    }

}
