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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBatchTest
@SpringBootTest(classes = BatchApplication.class)
@ActiveProfiles("test")
class DailyStockPriceJobConfigTest {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String STOCK_CODE = "005930";
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

        KisDailyPriceResponse.Output output = new KisDailyPriceResponse.Output(
                "20260910",
                "72000",
                "73500",
                "71000",
                "72800",
                "12345678"
        );

        KisDailyPriceResponse kisResponse = new KisDailyPriceResponse(
                "0",
                "MCA00000",
                "정상처리 되었습니다.",
                List.of(output)
        );

        given(kisAccessTokenProvider.getAccessToken())
                .willReturn(ACCESS_TOKEN);
        given(kisDailyPriceClient.fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE))
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
                .fetchDailyPrice(ACCESS_TOKEN, STOCK_CODE);


    }

    private Stock createStock() {
        return Stock.createStock(
                "00000A005930",
                "KR7005930003",
                STOCK_CODE,
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

}