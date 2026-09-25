package com.monow.batch.stock.dailyprice.job;

import com.monow.batch.BatchApplication;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.entity.StockPriceDaily;
import com.monow.domain.stock.repository.StockPriceDailyRepository;
import com.monow.domain.stock.repository.StockRepository;

import com.monow.external.kis.auth.application.KisAccessTokenProvider;
import com.monow.external.kis.stock.client.KisDailyPriceClient;
import com.monow.external.kis.stock.dto.response.KisDailyPriceResponse;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.print.attribute.standard.JobState;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.FLOAT_ARRAY;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBatchTest
@SpringBootTest(classes = BatchApplication.class)
class DailyStockPriceJobConfigTest {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String SAMSUNG_STOCK_CODE = "005930";
    private static final String SKHYNIX_STOCK_CODE = "000660";
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 9, 10);

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    @Qualifier("dailyStockPriceJob")
    private Job dailyStockPriceJob;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceDailyRepository stockPriceDailyRepository;

    @MockitoBean
    private KisDailyPriceClient kisDailyPriceClient;

    @MockitoBean
    private KisAccessTokenProvider kisAccessTokenProvider;

    @BeforeEach
    void setUp() {
        stockPriceDailyRepository.deleteAll();
        stockRepository.deleteAll();

        jobRepositoryTestUtils.removeJobExecutions();
        jobLauncherTestUtils.setJob(dailyStockPriceJob);
    }

    @Test
    @DisplayName("[성공] - 일별 시세 Batch Job 실행 시 신규 시세를 저장")
    void dailyStockPriceJob_whenDailyPriceDoesNotExist_savesNewDailyPrice() throws Exception {
        // Given
        Stock stock = createStock();
        stockRepository.save(stock);

        KisDailyPriceResponse kisResponse = createKisDailyPriceResponse();

        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);
        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, SAMSUNG_STOCK_CODE))
                .willReturn(kisResponse);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<StockPriceDaily> savedDailyPrices = stockPriceDailyRepository.findAll();

        assertThat(savedDailyPrices).hasSize(1);

        StockPriceDaily savedDailyPrice = savedDailyPrices.get(0);

        assertThat(savedDailyPrice.getTradeDate()).isEqualTo(TRADE_DATE);
        assertThat(savedDailyPrice.getOpenPrice()).isEqualByComparingTo("72000");
        assertThat(savedDailyPrice.getHighPrice()).isEqualByComparingTo("73500");
        assertThat(savedDailyPrice.getLowPrice()).isEqualByComparingTo("71000");
        assertThat(savedDailyPrice.getClosePrice()).isEqualByComparingTo("72800");
        assertThat(savedDailyPrice.getVolume()).isEqualTo(12345678L);

        verify(kisAccessTokenProvider, times(1)).getAccessToken();
        verify(kisDailyPriceClient, times(1))
                .fetchDailyPrice(ACCESS_TOKEN, SAMSUNG_STOCK_CODE);


    }

    @Test
    @DisplayName("[성공] - 일별 시세 데이터 오류 발생 시 해당 종목을 Skip하고 Job 완료")
    void dailyStockPriceJob_whenDailyPriceDataIsInvalid_skipsStockAndCompletes() throws Exception {
        // Given
        Stock samsung  = createStock();
        Stock skHynix   = Stock.createStock(
                "00000A000660",
                "KR7000660001",
                "000660",
                "SK하이닉스보통주",
                "SK하이닉스",
                DomesticStockMarketType.KOSPI,
                "300",
                "101010",
                "주권",
                "1010",
                "주식",
                true,
                true
        );

        stockRepository.saveAll(List.of(samsung, skHynix));

        KisDailyPriceResponse samsungResponse = createKisDailyPriceResponse();
        KisDailyPriceResponse skHynixInvalidResponse = new KisDailyPriceResponse(
                "0",
                "MCA00000",
                "정상처리 되었습니다.",
                List.of(
                        new KisDailyPriceResponse.Output(
                                "20260910",
                                "1600000",
                                "1650000",
                                "INVALID",
                                "1620000",
                                "9876543"
                        )
                )
        );

        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);
        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, SAMSUNG_STOCK_CODE))
                .willReturn(samsungResponse);
        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, SKHYNIX_STOCK_CODE))
                .willReturn(skHynixInvalidResponse);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<StockPriceDaily> savedDailyPrices = stockPriceDailyRepository.findAll();
        assertThat(savedDailyPrices).hasSize(1);

        List<StockPriceDaily> samsungDailyPrices = stockPriceDailyRepository.findByStockAndTradeDateIn(samsung, List.of(TRADE_DATE));
        assertThat(samsungDailyPrices).hasSize(1);

        List<StockPriceDaily> skHynixDailyPrices = stockPriceDailyRepository.findByStockAndTradeDateIn(skHynix, List.of(TRADE_DATE));
        assertThat(skHynixDailyPrices).isEmpty();

        StepExecution stepExecution = jobExecution.getStepExecutions().stream().findFirst().orElseThrow();
        assertThat(stepExecution.getSkipCount()).isEqualTo(1L);

    }

    @Test
    @DisplayName("[성공] - 일별 시세 Batch 실패 후 동일 JobInstance 재실행 시 Restart 완료")
    void dailyStockPriceJob_whenPreviousExecutionFailed_restartsAndCompletes() throws Exception {
        // Given
        // test를 위한 stock 15개 저장
        List<Stock> stocks = createRestartStocks(15);
        stockRepository.saveAll(stocks);

        KisDailyPriceResponse normalResponse = createKisDailyPriceResponse();

        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);

        for (Stock stock : stocks) {
            given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, stock.getStockCode()))
                    .willReturn(normalResponse);
        }

        // 12번째 종목만 첫 실행에서 강제 실패
        String failStockCode = stocks.get(11).getStockCode();
        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, failStockCode))
                .willThrow(new RuntimeException("Restart 테스트용 강제 실패"));

        LocalDate tradeDate = LocalDate.of(2026, 9, 24);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("tradeDate", tradeDate.toString())
                .toJobParameters();

        // When
        // 첫번째 Job 실행
        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        // 첫번째 실행은 강제 오류
        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.FAILED);

        // 첫번째 10건 저장 확인
        List<StockPriceDaily> firstSavedDailyPrices = stockPriceDailyRepository.findAll();
        assertThat(firstSavedDailyPrices).hasSize(10);

        // 기존에 실패하던 종목 정상 응답으로 변경
        willReturn(normalResponse)
                .given(kisDailyPriceClient)
                .fetchDailyPrice(ACCESS_TOKEN, failStockCode);

        // 첫 번째 실행과 동일한 JobParameters로 Restart
        JobExecution secondExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(secondExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(secondExecution.getJobInstance().getInstanceId())
                .isEqualTo(firstExecution.getJobInstance().getInstanceId());
        assertThat(secondExecution.getId()).isNotEqualTo(firstExecution.getId());

        // Restart 완료 후 최종적으로 모든 종목 저장
        List<StockPriceDaily> finalSavedDailyPrices = stockPriceDailyRepository.findAll();
        assertThat(finalSavedDailyPrices).hasSize(15);

    }

    private List<Stock> createRestartStocks(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> Stock.createStock(
                        "00000A" + String.format("%06d", index),
                        "KR7" + String.format("%09d", index),
                        String.format("%06d", index),
                        "테스트종목" + index + "보통주",
                        "테스트종목" + index,
                        DomesticStockMarketType.KOSPI,
                        "300",
                        "101010",
                        "주권",
                        "1010",
                        "주식",
                        true,
                        true
                ))
                .toList();

    }

    private Stock createStock() {
        return Stock.createStock(
                "00000A005930",
                "KR7005930003",
                SAMSUNG_STOCK_CODE,
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
    }

    private KisDailyPriceResponse createKisDailyPriceResponse() {
        KisDailyPriceResponse.Output output = new KisDailyPriceResponse.Output(
                "20260910",
                "72000",
                "73500",
                "71000",
                "72800",
                "12345678"
        );

        return new KisDailyPriceResponse(
                "0",
                "MCA00000",
                "정상처리 되었습니다.",
                List.of(output)
        );
    }

}