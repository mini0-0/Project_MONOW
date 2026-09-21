package com.monow.external.kis.stock.stockmaster;

import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.global.external.kis.stockmaster.KisStockMasterDownloader;
import com.monow.global.external.kis.stockmaster.KisStockMasterExtractor;
import com.monow.global.external.kis.stockmaster.KisStockMasterParser;
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
            Assertions.assertThat(result).isNotEmpty();

            Assertions.assertThat(result).containsKeys(
                    DomesticStockMarketType.KOSPI,
                    DomesticStockMarketType.KOSDAQ,
                    DomesticStockMarketType.NXT_KOSPI,
                    DomesticStockMarketType.NXT_KOSDAQ
            );
            for (DomesticStockMarketType marketType : DomesticStockMarketType.values()) {
                byte[] zipBytes = result.get(marketType);

                Assertions.assertThat(zipBytes).isNotNull();

                log.info("{} 종목 정보 zip 파일 다운로드 성공 - size: {} bytes", marketType, zipBytes.length);

                Assertions.assertThat(zipBytes.length).isGreaterThan(0);
                Assertions.assertThat(zipBytes[0]).isEqualTo((byte) 'P');
                Assertions.assertThat(zipBytes[1]).isEqualTo((byte) 'K');
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
            Assertions.assertThat(mstFiles).isNotEmpty();

            Assertions.assertThat(mstFiles).containsKeys(
                  DomesticStockMarketType.KOSPI,
                  DomesticStockMarketType.KOSDAQ,
                  DomesticStockMarketType.NXT_KOSPI,
                  DomesticStockMarketType.NXT_KOSDAQ
            );

            for (DomesticStockMarketType marketType : DomesticStockMarketType.values()) {
               byte[] mstBytes = mstFiles.get(marketType);

               Assertions.assertThat(mstBytes).isNotNull();
               Assertions.assertThat(mstBytes.length).isGreaterThan(0);

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
            Assertions.assertThat(stockCodes).isNotEmpty();

            Assertions.assertThat(stockCodes).containsKeys(
                    DomesticStockMarketType.KOSPI,
                    DomesticStockMarketType.KOSDAQ,
                    DomesticStockMarketType.NXT_KOSPI,
                    DomesticStockMarketType.NXT_KOSDAQ
            );

            for (DomesticStockMarketType marketType : DomesticStockMarketType.values()) {
                List<String> codes = stockCodes.get(marketType);

                Assertions.assertThat(codes).isNotNull();
                Assertions.assertThat(codes).isNotEmpty();
                Assertions.assertThat(codes).allMatch(code -> code.matches("\\d{6}"));

                log.info("{} stockCode count: {}", marketType, codes.size());
                log.info("{stockCode1: {}", codes.get(0));

            }



        }


    }
}
