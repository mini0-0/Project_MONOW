package com.monow.batch.stock.minuteprice.job;

import com.monow.batch.stock.minuteprice.partition.MinutePricePartitionService;
import com.monow.batch.stock.minuteprice.writer.MinuteStockPriceWriter;
import com.monow.domain.stock.model.StockPriceMinute;
import com.monow.external.kis.auth.application.KisAccessTokenProvider;
import com.monow.external.kis.config.KisProperties;
import com.monow.external.kis.exception.MinuteStockPriceRetryableException;
import com.monow.external.kis.stock.client.KisMinutePriceClient;
import com.monow.external.kis.stock.dto.request.KisMinutePriceRequest;
import com.monow.external.kis.stock.dto.response.KisMinutePriceResponse;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.Chunk;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers
class MinuteStockPricePostgresTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("monow_test")
            .withUsername("monow_test")
            .withPassword(UUID.randomUUID().toString());

    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private KisMinutePriceClient client;

    @BeforeEach
    void setUp() {
        String schema = "minute_test_" + UUID.randomUUID().toString().replace("-", "");
        String url = POSTGRES.getJdbcUrl();
        String username = POSTGRES.getUsername();
        String password = POSTGRES.getPassword();
        new JdbcTemplate(new DriverManagerDataSource(url, username, password)).execute("CREATE SCHEMA " + schema);

        String separator = url.contains("?") ? "&" : "?";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                url + separator + "currentSchema=" + schema, username, password);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE stocks (id BIGINT PRIMARY KEY, stock_code VARCHAR(20), krx_tradable BOOLEAN, nxt_tradable BOOLEAN, is_active BOOLEAN)");
        jdbc.update("INSERT INTO stocks VALUES (100, '005930', true, false, true), (200, '000660', false, true, true)");
        new ResourceDatabasePopulator(new ClassPathResource("org/springframework/batch/core/schema-postgresql.sql"))
                .execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("db/stock-price-minute.sql")).execute(dataSource);

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "monow.batch.minute-price.enabled", "true",
                "monow.batch.minute-partition.enabled", "true",
                "monow.batch.minute-price.request-interval-ms", "50",
                "monow.batch.minute-price.retry-delay-ms", "1")));
        client = mock(KisMinutePriceClient.class);
        KisAccessTokenProvider tokens = mock(KisAccessTokenProvider.class);
        when(tokens.getAccessToken()).thenReturn("test-token");
        context.registerBean("dataSource", DataSource.class, () -> dataSource);
        context.registerBean("transactionManager", PlatformTransactionManager.class,
                () -> new DataSourceTransactionManager(dataSource));
        context.registerBean(JdbcTemplate.class, () -> jdbc);
        context.registerBean(Clock.class, () -> Clock.fixed(Instant.parse("2027-01-15T12:00:00Z"), ZoneId.of("Asia/Seoul")));
        context.registerBean(KisProperties.class, KisProperties::new);
        context.registerBean(KisMinutePriceClient.class, () -> client);
        context.registerBean(KisAccessTokenProvider.class, () -> tokens);
        context.register(TestBatchConfiguration.class, MinuteStockPriceJobConfig.class, MinutePricePartitionJobConfig.class);
        context.refresh();
        partitions().ensurePartition(YearMonth.of(2026, 11));
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Configuration
    @EnableBatchProcessing
    @EnableTransactionManagement
    static class TestBatchConfiguration {
    }

    @Test
    @DisplayName("[성공] - 월별 파티션을 반복 생성하고 한국 시간 월 경계에 맞게 저장")
    void givenMonthlyPartitions_whenWriteAcrossBoundary_thenRoutesWithoutDuplicateIndexes() {
        partitions().ensurePartition(YearMonth.of(2026, 11));
        partitions().ensurePartition(YearMonth.of(2026, 12));
        write(price(100, "2026-11-30T15:30:00+09:00", 101, "2027-01-01T00:00:00Z"),
                price(100, "2026-12-01T09:00:00+09:00", 101, "2027-01-01T00:00:00Z"));
        List<String> tables = jdbc.queryForList("SELECT tableoid::regclass::text FROM stock_price_minute ORDER BY bar_at", String.class);
        assertThat(tables.get(0)).endsWith("stock_price_minute_202611");
        assertThat(tables.get(1)).endsWith("stock_price_minute_202612");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_minute", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("[성공] - 동일 값 재입력은 갱신하지 않고 최신 변경값만 반영")
    void givenExistingPrice_whenUpsertRepeatedAndChangedValues_thenPreservesUnchangedAndRejectsStaleValues() {
        write(price(100, "2026-11-11T09:00:00+09:00", 101, "2027-01-01T00:00:00Z"));
        write(price(100, "2026-11-11T09:00:00+09:00", 101, "2027-01-02T00:00:00Z"));
        assertThat(jdbc.queryForObject("SELECT collected_at FROM stock_price_minute", OffsetDateTime.class).toInstant())
                .isEqualTo(Instant.parse("2027-01-01T00:00:00Z"));
        write(price(100, "2026-11-11T09:00:00+09:00", 102, "2027-01-03T00:00:00Z"));
        write(price(100, "2026-11-11T09:00:00+09:00", 100, "2027-01-01T00:00:00Z"));
        assertThat(jdbc.queryForObject("SELECT close_price FROM stock_price_minute", BigDecimal.class)).isEqualByComparingTo("102");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_minute", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("[실패] - JDBC 배치 중 제약조건 오류가 발생하면 전체 청크를 롤백")
    void givenInvalidForeignKey_whenWrite_thenRollsBackEntireChunk() {
        assertThatThrownBy(() -> write(
                price(100, "2026-11-11T09:00:00+09:00", 101, "2027-01-01T00:00:00Z"),
                price(999, "2026-11-11T09:00:00+09:00", 101, "2027-01-01T00:00:00Z")))
                .isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_minute", Integer.class)).isZero();
    }

    @Test
    @DisplayName("[실패] - 누락된 파티션은 API 호출 전에 실패")
    void givenMissingPartition_whenLaunch_thenFailsBeforeApiCall() throws Exception {
        JobExecution execution = launch("2026-12-01", "0");
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("[실패] - 같은 이름의 일반 테이블을 파티션으로 오인하지 않음")
    void givenUnattachedTable_whenEnsurePartition_thenRejectsIt() {
        jdbc.execute("CREATE TABLE stock_price_minute_202612 (id BIGINT)");
        assertThatThrownBy(() -> partitions().ensurePartition(YearMonth.of(2026, 12)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("[실패] - 부모에 연결되어 있어도 잘못된 월 경계 파티션을 거부")
    void givenWrongPartitionBounds_whenRequirePartition_thenRejectsIt() {
        jdbc.execute("""
                CREATE TABLE stock_price_minute_202612 PARTITION OF stock_price_minute
                FOR VALUES FROM ('2027-01-01 00:00:00+09') TO ('2027-02-01 00:00:00+09')
                """);
        assertThatThrownBy(() -> partitions().requirePartition(YearMonth.of(2026, 12)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("[성공] - 파티션 관리 Job은 지정 월만 준비하고 KIS API를 호출하지 않음")
    void givenPartitionMaintenanceJob_whenLaunch_thenPreparesRequestedMonth() throws Exception {
        JobParameters parameters = new JobParametersBuilder().addString("month", "2026-12").toJobParameters();
        JobExecution execution = context.getBean(JobLauncher.class)
                .run(context.getBean("minutePricePartitionJob", Job.class), parameters);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        partitions().requirePartition(YearMonth.of(2026, 12));
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("[실패] - 일시 오류 재시도 소진 시 종목을 Skip하지 않고 Job 실패")
    void givenExhaustedRetries_whenLaunch_thenFailsWithoutSavingOrSkipping() throws Exception {
        when(client.fetchMinutePrice(anyString(), any()))
                .thenThrow(new MinuteStockPriceRetryableException("temporary", new RuntimeException()));
        JobExecution execution = launch("2026-11-11", "0");
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getStepExecutions()).allMatch(step -> step.getSkipCount() == 0);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_minute", Integer.class)).isZero();
        verify(client, times(3)).fetchMinutePrice(anyString(), any());
    }

    @Test
    @DisplayName("[성공] - 실패 후 동일 JobInstance를 재시작하고 대상 목록 변경에도 누락과 중복을 방지")
    void givenFailedCollection_whenRestart_thenResumesCommittedCheckpointWithOriginalTargets() throws Exception {
        when(client.fetchMinutePrice(anyString(), any())).thenAnswer(invocation -> {
            KisMinutePriceRequest request = invocation.getArgument(1);
            if (request.stockCode().equals("000660")) {
                throw new IllegalStateException("test permanent failure");
            }
            return response();
        });
        JobExecution first = launch("2026-11-11", "0");
        assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_minute", Integer.class)).isEqualTo(2);
        jdbc.update("UPDATE stocks SET stock_code = 'CHANGED' WHERE id = 200");
        jdbc.update("INSERT INTO stocks VALUES (300, 'NEW', true, true, true)");
        doReturn(response()).when(client).fetchMinutePrice(anyString(),
                argThat(request -> request != null && request.stockCode().equals("000660")));
        JobExecution second = launch("2026-11-11", "0");
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(second.getJobInstance().getId()).isEqualTo(first.getJobInstance().getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_minute", Integer.class)).isEqualTo(4);
        verify(client, times(1)).fetchMinutePrice(anyString(), argThat(request -> request.stockCode().equals("005930")));
        verify(client, times(2)).fetchMinutePrice(anyString(), argThat(request -> request.stockCode().equals("000660")));
        verify(client, never()).fetchMinutePrice(anyString(), argThat(request -> request.stockCode().equals("NEW")));
    }

    @Test
    @DisplayName("[성공] - 일시 오류를 재시도하고 완료 후 새 Revision도 중복 없이 적재")
    void givenTemporaryFailure_whenRetryAndRecollect_thenCompletesWithoutDuplicates() throws Exception {
        when(client.fetchMinutePrice(anyString(), any()))
                .thenThrow(new MinuteStockPriceRetryableException("temporary", new RuntimeException()))
                .thenReturn(response());
        assertThat(launch("2026-11-11", "0").getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(launch("2026-11-11", "1").getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_minute", Integer.class)).isEqualTo(4);
        verify(client, times(5)).fetchMinutePrice(anyString(), any());
    }

    @Test
    @DisplayName("[성공] - 활성 KRX 또는 NXT 종목을 합쳐 한 번씩 수집하고 UN으로 저장")
    void givenMixedMarketTargets_whenCollect_thenStoresIntegratedPricesOncePerStock() throws Exception {
        jdbc.update("UPDATE stocks SET nxt_tradable = true WHERE id = 100");
        jdbc.update("""
                INSERT INTO stocks VALUES
                (300, 'KRXONLY', true, false, true),
                (400, 'NEITHER', false, false, true),
                (500, 'INACTIVE', true, true, false)
                """);
        when(client.fetchMinutePrice(anyString(), any())).thenReturn(response());

        assertThat(launch("2026-11-11", "0").getStatus()).isEqualTo(BatchStatus.COMPLETED);

        assertThat(jdbc.queryForList("SELECT DISTINCT stock_id FROM stock_price_minute ORDER BY stock_id", Long.class))
                .containsExactly(100L, 200L, 300L);
        assertThat(jdbc.queryForList("SELECT DISTINCT market_code FROM stock_price_minute", String.class))
                .containsExactly("UN");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_minute", Integer.class)).isEqualTo(6);

        var requests = org.mockito.ArgumentCaptor.forClass(KisMinutePriceRequest.class);
        verify(client, times(3)).fetchMinutePrice(anyString(), requests.capture());
        assertThat(requests.getAllValues()).extracting(KisMinutePriceRequest::stockCode)
                .containsExactly("005930", "000660", "KRXONLY");
    }

    @Test
    @DisplayName("[성공] - UN 보정 저장은 기존 J 분봉을 변경하거나 통합 분봉으로 바꾸지 않음")
    void givenExistingKrxPrice_whenUpdateIntegratedPrice_thenPreservesOldMarketData() {
        write(price(100, "2026-11-11T09:00:00+09:00", 101, "2027-01-01T00:00:00Z"));
        jdbc.update("""
                INSERT INTO stock_price_minute (
                    stock_id, market_code, bar_at, open_price, high_price, low_price, close_price, volume, collected_at
                ) SELECT stock_id, 'J', bar_at, open_price, high_price, low_price, close_price, volume, collected_at
                  FROM stock_price_minute WHERE market_code = 'UN'
                """);

        write(price(100, "2026-11-11T09:00:00+09:00", 102, "2027-01-02T00:00:00Z"));

        assertThat(jdbc.queryForObject("SELECT close_price FROM stock_price_minute WHERE market_code = 'J'", BigDecimal.class))
                .isEqualByComparingTo("101");
        assertThat(jdbc.queryForObject("SELECT close_price FROM stock_price_minute WHERE market_code = 'UN'", BigDecimal.class))
                .isEqualByComparingTo("102");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_price_minute", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("[실패] - 서로 다른 실행도 동일 분봉 수집 잠금을 동시에 확보하지 못함")
    void givenRunningCollector_whenAcquireAnotherLock_thenRejectsUntilReleased() {
        MinutePriceExecutionLock lock = context.getBean(MinutePriceExecutionLock.class);
        JobExecution first = new JobExecution(1001L);
        JobExecution second = new JobExecution(1002L);
        lock.beforeJob(first);
        try {
            assertThatThrownBy(() -> lock.beforeJob(second)).isInstanceOf(IllegalStateException.class);
        } finally {
            lock.afterJob(first);
        }
        lock.beforeJob(second);
        lock.afterJob(second);
    }

    private JobExecution launch(String date, String revision) throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addString("tradeDate", date).addString("marketCode", "UN").addString("session", "INTEGRATED_DAY")
                .addString("startTime", "09:00:00").addString("endTime", "09:01:00")
                .addString("collectionRevision", revision).toJobParameters();
        return context.getBean(JobLauncher.class).run(context.getBean("minuteStockPriceJob", Job.class), parameters);
    }

    private MinutePricePartitionService partitions() {
        return context.getBean("minutePricePartitionService", MinutePricePartitionService.class);
    }

    private void write(StockPriceMinute... prices) {
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
            try {
                context.getBean(MinuteStockPriceWriter.class).write(new Chunk<>(List.of(List.of(prices))));
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private StockPriceMinute price(long stockId, String barAt, int close, String collectedAt) {
        return new StockPriceMinute(stockId, "UN", OffsetDateTime.parse(barAt), BigDecimal.valueOf(100),
                BigDecimal.valueOf(103), BigDecimal.valueOf(99), BigDecimal.valueOf(close), 10,
                OffsetDateTime.parse(collectedAt));
    }

    private KisMinutePriceResponse response() {
        return new KisMinutePriceResponse("0", "OK", "", List.of(
                new KisMinutePriceResponse.Output("20261111", "090100", "100", "103", "99", "101", "10"),
                new KisMinutePriceResponse.Output("20261111", "090000", "100", "103", "99", "101", "10")));
    }
}
