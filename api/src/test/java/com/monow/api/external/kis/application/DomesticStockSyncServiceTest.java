package com.monow.api.external.kis.application;


import com.monow.domain.stock.entity.DomesticStockMarketType;
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
        @DisplayName("[성공] - 기존에 저장된 종목을 제외하고 신규 종목의 시장별 거래 가능 여부와 함께 동기화")
        void syncDomesticStocks_whenStockCodesParsed_syncDistinctStockCodes() {
            // Given
            Map<DomesticStockMarketType, byte[]> zipFiles = new EnumMap<>(DomesticStockMarketType.class);
            zipFiles.put(DomesticStockMarketType.KOSPI, new byte[]{1});
            zipFiles.put(DomesticStockMarketType.KOSDAQ, new byte[]{2});
            zipFiles.put(DomesticStockMarketType.NXT_KOSPI, new byte[]{3});
            zipFiles.put(DomesticStockMarketType.NXT_KOSDAQ, new byte[]{4});

            Map<DomesticStockMarketType, byte[]> mstFiles = new EnumMap<>(DomesticStockMarketType.class);
            mstFiles.put(DomesticStockMarketType.KOSPI, new byte[]{5});
            mstFiles.put(DomesticStockMarketType.KOSDAQ, new byte[]{6});
            mstFiles.put(DomesticStockMarketType.NXT_KOSPI, new byte[]{7});
            mstFiles.put(DomesticStockMarketType.NXT_KOSDAQ, new byte[]{8});

            Map<DomesticStockMarketType, List<String>> stockCodes = new EnumMap<>(DomesticStockMarketType.class);

            stockCodes.put(
                    DomesticStockMarketType.KOSPI,
                    List.of("005930", "000660")
            );
            stockCodes.put(
                    DomesticStockMarketType.KOSDAQ,
                    List.of("035720")
            );
            stockCodes.put(
                    DomesticStockMarketType.NXT_KOSPI,
                    List.of("005930")
            );

            stockCodes.put(
                    DomesticStockMarketType.NXT_KOSDAQ,
                    List.of("035720")
            );

            Stock existingStock = Stock.createStock(
                    "00000A005930",
                    "KR7005930003",
                    "005930",
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

            given(kisStockMasterDownloader.downloaderDomesticStock())
                    .willReturn(zipFiles);

            given(kisStockMasterExtractor.extractMstFiles(zipFiles))
                    .willReturn(mstFiles);

            given(kisStockMasterParser.parseStockCodes(mstFiles))
                    .willReturn(stockCodes);

            given(stockRepository.findByStockCodeIn(anyCollection()))
                    .willReturn(List.of(existingStock));

            // When
            domesticStockSyncService.syncDomesticStocks();

            // Then
            verify(kisStockMasterDownloader, times(1))
                    .downloaderDomesticStock();

            verify(kisStockMasterExtractor, times(1))
                    .extractMstFiles(zipFiles);

            verify(kisStockMasterParser, times(1))
                    .parseStockCodes(mstFiles);

            verify(stockInfoSyncService, never()).syncStockInfo(
                    eq("005930"),
                    any(DomesticStockMarketType.class),
                    anyBoolean(),
                    anyBoolean()
            );
            verify(stockInfoSyncService).syncStockInfo(
                    "000660",
                    DomesticStockMarketType.KOSPI,
                    true,
                    false
            );
            verify(stockInfoSyncService).syncStockInfo(
                    "035720",
                    DomesticStockMarketType.KOSDAQ,
                    true,
                    true
            );
            verify(stockInfoSyncService, times(2)).syncStockInfo(
                    anyString(),
                    any(DomesticStockMarketType.class),
                    anyBoolean(),
                    anyBoolean()
            );
        }

        @Nested
        @DisplayName("선택 국내주식 종목 동기화")
        class SyncSelectedDomesticStocks {

            @Test
            @DisplayName("[성공] - 중복 요청 종목은 한 번만 처리하고 마스터 파일도 한 번만 조회")
            void givenDuplicateStockCodes_whenSyncSelectedDomesticStocks_thenSyncEachStockOnce() {
                // Given
                Map<DomesticStockMarketType, byte[]> zipFiles = new EnumMap<>(DomesticStockMarketType.class);

                zipFiles.put(DomesticStockMarketType.KOSPI, new byte[]{1});
                zipFiles.put(DomesticStockMarketType.KOSDAQ, new byte[]{2});
                zipFiles.put(DomesticStockMarketType.NXT_KOSPI, new byte[]{3});
                zipFiles.put(DomesticStockMarketType.NXT_KOSDAQ, new byte[]{4});

                Map<DomesticStockMarketType, byte[]> mstFiles = new EnumMap<>(DomesticStockMarketType.class);

                mstFiles.put(DomesticStockMarketType.KOSPI, new byte[]{5});
                mstFiles.put(DomesticStockMarketType.KOSDAQ, new byte[]{6});
                mstFiles.put(DomesticStockMarketType.NXT_KOSPI, new byte[]{7});
                mstFiles.put(DomesticStockMarketType.NXT_KOSDAQ, new byte[]{8});

                Map<DomesticStockMarketType, List<String>> stockCodes = new EnumMap<>(DomesticStockMarketType.class);

                stockCodes.put(
                        DomesticStockMarketType.KOSPI,
                        List.of("005930", "000660")
                );

                stockCodes.put(
                        DomesticStockMarketType.KOSDAQ,
                        List.of("035720")
                );

                stockCodes.put(
                        DomesticStockMarketType.NXT_KOSPI,
                        List.of("005930")
                );

                stockCodes.put(
                        DomesticStockMarketType.NXT_KOSDAQ,
                        List.of("035720")
                );

                given(kisStockMasterDownloader.downloaderDomesticStock())
                        .willReturn(zipFiles);

                given(kisStockMasterExtractor.extractMstFiles(zipFiles))
                        .willReturn(mstFiles);

                given(kisStockMasterParser.parseStockCodes(mstFiles))
                        .willReturn(stockCodes);

                given(stockRepository.findByStockCodeIn(anyCollection()))
                        .willReturn(List.of());

                List<String> requestedStockCodes =
                        List.of("000660", "000660", "035720");

                // When
                domesticStockSyncService.syncSelectedDomesticStocks(
                        requestedStockCodes
                );

                // Then
                verify(kisStockMasterDownloader, times(1))
                        .downloaderDomesticStock();

                verify(stockInfoSyncService, times(1)).syncStockInfo(
                        "000660",
                        DomesticStockMarketType.KOSPI,
                        true,
                        false
                );

                verify(stockInfoSyncService, times(1)).syncStockInfo(
                        "035720",
                        DomesticStockMarketType.KOSDAQ,
                        true,
                        true
                );

                verify(stockInfoSyncService, times(2)).syncStockInfo(
                        anyString(),
                        any(DomesticStockMarketType.class),
                        anyBoolean(),
                        anyBoolean()
                );
            }

        }
    }

}
