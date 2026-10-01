package com.monow.batch.stock.master.job;

import com.monow.batch.BatchApplication;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.kis.auth.application.KisAccessTokenProvider;
import com.monow.external.kis.exception.DomesticStockMasterRetryableException;
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

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

@SpringBatchTest
@SpringBootTest(classes = BatchApplication.class)
@ActiveProfiles("test")
class DomesticStockMasterJobConfigTest {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String STOCK_CODE = "005930";

    private static final String INVALID_STOCK_CODE = "000660";

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 9, 1);

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
        Map<DomesticStockMarketType, byte[]> zipFiles = createZipFiles();
        // .mst 파일
        Map<DomesticStockMarketType, byte[]> mstFiles = createMstFiles();

        Map<DomesticStockMarketType, List<String>> stockCodes = createStockCode(STOCK_CODE);

        KisStockInfoResponse stockInfoResponse = createKisStockInfoResponse();

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
                .addString("tradeDate", TRADE_DATE.toString())
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

    @Test
    @DisplayName("[성공] - 오류 종목 발생 시 해당 종목을 Skip하고 정상 종목을 저장한다")
    void domesticStockMasterJob_whenInvalidStockExists_skipsInvalidStockAndCompletes() throws Exception {
        // Given
        // zip 파일
        Map<DomesticStockMarketType, byte[]> zipFiles = createZipFiles();
        // .mst 파일
        Map<DomesticStockMarketType, byte[]> mstFiles = createMstFiles();

        Map<DomesticStockMarketType, List<String>> stockCodes = createStockCodes(List.of(STOCK_CODE, INVALID_STOCK_CODE));

        KisStockInfoResponse stockInfoResponse = createKisStockInfoResponse();

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
                .addString("tradeDate", TRADE_DATE.toString())
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

        assertThat(stockRepository.findByStockCode(INVALID_STOCK_CODE)).isEmpty();

        StepExecution stepExecution = jobExecution.getStepExecutions().stream().findFirst().orElseThrow();
        assertThat(stepExecution.getSkipCount()).isEqualTo(1L);

    }

    @Test
    @DisplayName("[성공] - 종목 Master Batch 실패 후 동일 JobInstance 재실행 시 Restart 완료")
    void domesticStockMasterJob_whenPreviousExecutionFailed_restartsAndCompletes() throws Exception {
        // Given
        // zip 파일
        Map<DomesticStockMarketType, byte[]> zipFiles = createZipFiles();
        // .mst 파일
        Map<DomesticStockMarketType, byte[]> mstFiles = createMstFiles();

        List<String> stockCodes = List.of(
                "000001",
                "000002",
                "000003",
                "000004",
                "000005",
                "000006",
                "000007",
                "000008",
                "000009",
                "000010",
                "000011"
        );

        Map<DomesticStockMarketType, List<String>> parsedStockCodes = createStockCodes(stockCodes);

        given(kisStockMasterDownloader.downloaderDomesticStock())
                .willReturn(zipFiles);

        given(kisStockMasterExtractor.extractMstFiles(zipFiles))
                .willReturn(mstFiles);

        given(kisStockMasterParser.parseStockCodes(mstFiles))
                .willReturn(parsedStockCodes );

        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);

        for (int i = 0; i < 10; i++) {
            String stockCode = stockCodes.get(i);

            given(kisStockInfoClient.fetchStockInfo(ACCESS_TOKEN, stockCode))
                    .willReturn(createKisStockInfoResponse(stockCode));
        }

        String failedStockCode = stockCodes.get(10);

        given(kisStockInfoClient.fetchStockInfo(ACCESS_TOKEN, failedStockCode))
                .willThrow(new DomesticStockMasterRetryableException("KIS 일시 오류"));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tradeDate", TRADE_DATE.toString())
                .toJobParameters();


        // When
        // 첫번째 Job 실행
        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        // 첫번째 실행은 강제 오류
        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.FAILED);

        // 첫번쩨 10건 저장 확인
        List<Stock> firstSavedStocks = stockRepository.findAll();
        assertThat(firstSavedStocks).hasSize(10);

        // 기존에 실패하던 StockCode 정상 응답으로 변경
        willReturn(createKisStockInfoResponse(failedStockCode))
                .given(kisStockInfoClient)
                .fetchStockInfo(ACCESS_TOKEN, failedStockCode);

        // 첫 번째 실행과 동일한 JobParameters로 Restart
        JobExecution secondExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(secondExecution.getJobInstance().getInstanceId())
                .isEqualTo(firstExecution.getJobInstance().getInstanceId());
        assertThat(secondExecution.getId()).isNotEqualTo(firstExecution.getId());

        // Restart 완료 후 최종적으로 모든 종목 저장
        List<Stock> finalSavedStocks  = stockRepository.findAll();
        assertThat(finalSavedStocks ).hasSize(11);

    }


    private Map<DomesticStockMarketType, byte[]> createZipFiles() {
        Map<DomesticStockMarketType, byte[]> zipFiles = new EnumMap<>(DomesticStockMarketType.class);

        zipFiles.put(
                DomesticStockMarketType.KOSPI,
                new byte[]{1}
        );

        return zipFiles;
    }

    private Map<DomesticStockMarketType, byte[]> createMstFiles() {
        Map<DomesticStockMarketType, byte[]> mstFiles = new EnumMap<>(DomesticStockMarketType.class);
        mstFiles.put(
                DomesticStockMarketType.KOSPI,
                new byte[]{2}
        );

        return mstFiles;
    }

    private Map<DomesticStockMarketType, List<String>> createStockCode(String stockCode) {
        Map<DomesticStockMarketType, List<String>> stockCodes = new EnumMap<>(DomesticStockMarketType.class);

        List<String> codes = List.of(stockCode);

        stockCodes.put(DomesticStockMarketType.KOSPI, codes);
        stockCodes.put(DomesticStockMarketType.KOSDAQ, List.of());
        stockCodes.put(DomesticStockMarketType.NXT_KOSPI, codes);
        stockCodes.put(DomesticStockMarketType.NXT_KOSDAQ, List.of());

        return stockCodes;
    }

    private Map<DomesticStockMarketType, List<String>> createStockCodes(List<String> stockCodes) {
        Map<DomesticStockMarketType, List<String>> result = new EnumMap<>(DomesticStockMarketType.class);

        result.put(DomesticStockMarketType.KOSPI, stockCodes);
        result.put(DomesticStockMarketType.KOSDAQ, List.of());
        result.put(DomesticStockMarketType.NXT_KOSPI, List.of());
        result.put(DomesticStockMarketType.NXT_KOSDAQ, List.of());

        return result;
    }


    private KisStockInfoResponse createKisStockInfoResponse() {
        KisStockInfoResponse.Output output = new KisStockInfoResponse.Output(
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

        return new KisStockInfoResponse(
                "0",
                "MCA00000",
                "정상처리 되었습니다.",
                output
        );

    }

    private KisStockInfoResponse createKisStockInfoResponse(String stockCode) {
        KisStockInfoResponse.Output output = new KisStockInfoResponse.Output(
                "00000A" + stockCode,
                "KR7" + stockCode,
                stockCode,
                "테스트종목" + stockCode,
                "테스트종목" + stockCode,
                "300",
                "101010",
                "주권",
                "1010",
                "주식"
        );

        return new KisStockInfoResponse(
                "0",
                "MCA00000",
                "정상처리 되었습니다.",
                output
        );
    }

}


