package com.monow.batch.stock.master.job;

import com.monow.batch.BatchApplication;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.kis.auth.application.KisAccessTokenProvider;
import com.monow.external.kis.stock.client.KisStockInfoClient;
import com.monow.external.kis.stock.dto.response.KisStockInfoResponse;
import com.monow.external.kis.stock.stockmaster.KisStockMasterDownloader;
import com.monow.external.kis.stock.stockmaster.KisStockMasterExtractor;
import com.monow.external.kis.stock.stockmaster.KisStockMasterParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;


@SpringBatchTest
@SpringBootTest(classes = BatchApplication.class)
@ActiveProfiles("test")
class DomesticStockMasterJobConfigTest {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String STOCK_CODE = "005930";

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    @Qualifier("domesticStockMasterJob")
    private Job domesticStockMasterJob;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceDailyRepository stockPriceDailyRepository;

    @MockitoBean
    private KisStockMasterDownloader kisStockMasterDownloader;

    @MockitoBean
    private KisStockMasterExtractor kisStockMasterExtractor;

    @MockitoBean
    private KisStockMasterParser kisStockMasterParser;

    @MockitoBean
    private KisAccessTokenProvider kisAccessTokenProvider;

    @MockitoBean
    private KisStockInfoClient kisStockInfoClient;

    @BeforeEach
    void setUp() {
        stockPriceDailyRepository.deleteAll();
        stockRepository.deleteAll();

        jobRepositoryTestUtils.removeJobExecutions();

        jobLauncherTestUtils.setJob(domesticStockMasterJob);
    }


    @Test
    @DisplayName("[성공] - 종목 Master Batch 실행 시 신규 종목을 저장한다")
    void domesticStockMasterJob_whenNewStockExists_savesStock() throws Exception {
        // Given
        // zip 파일
        Map<DomesticStockMarketType, byte[]> zipFiles = new EnumMap<>(DomesticStockMarketType.class);
        zipFiles.put(
                DomesticStockMarketType.KOSPI,
                new byte[]{1}
        );

        // .mst 파일
        Map<DomesticStockMarketType, byte[]> mstFiles = new EnumMap<>(DomesticStockMarketType.class);
        mstFiles.put(
                DomesticStockMarketType.KOSPI,
                new byte[]{2}
        );

        Map<DomesticStockMarketType, List<String>> stockCodes = new EnumMap<>(DomesticStockMarketType.class);

        stockCodes.put(
                DomesticStockMarketType.KOSPI,
                List.of(STOCK_CODE)
        );

        stockCodes.put(
                DomesticStockMarketType.KOSDAQ,
                List.of()
        );

        stockCodes.put(
                DomesticStockMarketType.NXT_KOSPI,
                List.of(STOCK_CODE)
        );

        stockCodes.put(
                DomesticStockMarketType.NXT_KOSDAQ,
                List.of()
        );

        KisStockInfoResponse.Output output =
                new KisStockInfoResponse.Output(
                        "00000A005930",
                        "KR7005930003",
                        "005930",
                        "삼성전자보통주",
                        "삼성전자",
                        "300",
                        "101010",
                        "주권",
                        "1010",
                        "주식"
                );

        KisStockInfoResponse stockInfoResponse =
                new KisStockInfoResponse(
                        "0",
                        "MCA00000",
                        "정상처리 되었습니다.",
                        output
                );

        given(kisStockMasterDownloader.downloaderDomesticStock())
                .willReturn(zipFiles);

        given(kisStockMasterExtractor.extractMstFiles(zipFiles))
                .willReturn(mstFiles);

        given(kisStockMasterParser.parseStockCodes(mstFiles))
                .willReturn(stockCodes);

        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);

        given(kisStockInfoClient.fetchStockInfo(ACCESS_TOKEN, STOCK_CODE))
                .willReturn(stockInfoResponse);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();



        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Stock savedStock = stockRepository.findByStockCode(STOCK_CODE)
                .orElseThrow();

        assertThat(savedStock.getStockCode())
                .isEqualTo(STOCK_CODE);

        assertThat(savedStock.getStockName())
                .isEqualTo("삼성전자");

        assertThat(savedStock.getMarketType())
                .isEqualTo(DomesticStockMarketType.KOSPI);


    }

}