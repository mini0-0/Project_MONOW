package com.monow.api.external.kis.stockmaster;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class KisStockMasterSyncIntegrationTest {

    @Autowired
    private KisStockMasterDownloader kisStockMasterDownloader;

    @Autowired
    private KisStockMasterExtractor kisStockMasterExtractor;

    @Autowired
    private KisStockMasterParser kisStockMasterParser;

    @Nested
    @DisplayName("한국투자증권 국내 주식 종목 정보 파일 다운로드")
    class KisStockDownloader {

        @Test
        @DisplayName("[성공] - 국내주식 정보 파일 zip 파일들 다운로드")
        void downloadDomesticStocks_whenRequestSucceeds_returnsZipFiles() {
            // Given


            // When
            Map<DomesticStockMarketType, byte[]> result =
                    kisStockMasterDownloader.downloaderDomesticStock();

            // Then
            assertThat(result).isNotEmpty();

            assertThat(result).containsKeys(
                    DomesticStockMarketType.KOSPI,
                    DomesticStockMarketType.KOSDAQ,
                    DomesticStockMarketType.NXT_KOSPI,
                    DomesticStockMarketType.NXT_KOSDAQ
            );
            for (DomesticStockMarketType marketType : DomesticStockMarketType.values()) {
                byte[] zipBytes = result.get(marketType);

                assertThat(zipBytes).isNotNull();

                log.info("{} 종목 정보 zip 파일 다운로드 성공 - size: {} bytes", marketType, zipBytes.length);

                assertThat(zipBytes.length).isGreaterThan(0);
                assertThat(zipBytes[0]).isEqualTo((byte) 'P');
                assertThat(zipBytes[1]).isEqualTo((byte) 'K');
            }

        }

    }


    @Nested
    @DisplayName("한국투자증권 국내주식 종목 정보 파일 압축 해제 ")
    class ExtractDomesticStockFiles {

        @Test
        @DisplayName("[성공] - 국내주식 zip 파일에서 mst 파일을 추출")
        void extractDomesticStockFiles_whenZipFilesDownloaded_returnsMstFiles() {
            // Given
            Map<DomesticStockMarketType, byte[]> zipFiles =
                    kisStockMasterDownloader.downloaderDomesticStock();

            // When
            Map<DomesticStockMarketType, byte[]> mstFiles =
                    kisStockMasterExtractor.extractMstFiles(zipFiles);

            // Then
            assertThat(mstFiles).isNotEmpty();

            assertThat(mstFiles).containsKeys(
                  DomesticStockMarketType.KOSPI,
                  DomesticStockMarketType.KOSDAQ,
                  DomesticStockMarketType.NXT_KOSPI,
                  DomesticStockMarketType.NXT_KOSDAQ
            );

            for (DomesticStockMarketType marketType : DomesticStockMarketType.values()) {
               byte[] mstBytes = mstFiles.get(marketType);

               assertThat(mstBytes).isNotNull();
               assertThat(mstBytes.length).isGreaterThan(0);

                log.info("{} mst file size: {} bytes", marketType, mstBytes.length);
            }

        }

        @Test
        @DisplayName("[성공] - mst 파일에서 국내주식 종목코드 목록을 추출")
        void parseStockCodes_whenMstFilesExtracted_returnsStockCodes() {
            // Given
            Map<DomesticStockMarketType, byte[]> zipFiles =
                    kisStockMasterDownloader.downloaderDomesticStock();

            Map<DomesticStockMarketType, byte[]> mstFiles =
                    kisStockMasterExtractor.extractMstFiles(zipFiles);

            // When
            Map<DomesticStockMarketType, List<String>> stockCodes =
                    kisStockMasterParser.parseStockCodes(mstFiles);

            // Then
            assertThat(stockCodes).isNotEmpty();

            assertThat(stockCodes).containsKeys(
                    DomesticStockMarketType.KOSPI,
                    DomesticStockMarketType.KOSDAQ,
                    DomesticStockMarketType.NXT_KOSPI,
                    DomesticStockMarketType.NXT_KOSDAQ
            );

            for (DomesticStockMarketType marketType : DomesticStockMarketType.values()) {
                List<String> codes = stockCodes.get(marketType);

                assertThat(codes).isNotNull();
                assertThat(codes).isNotEmpty();
                assertThat(codes).allMatch(code -> code.matches("\\d{6}"));

                log.info("{} stockCode count: {}", marketType, codes.size());
                log.info("{stockCode1: {}", codes.get(0));

            }



        }


    }
}
