package com.monow.batch.dart.corpcode;

import com.monow.batch.dart.corpcode.StockCorpCodeMappingService.FailureReason;
import com.monow.batch.dart.corpcode.StockCorpCodeMappingService.MappingFailure;
import com.monow.batch.dart.corpcode.StockCorpCodeMappingService.MappingResult;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.dart.corpcode.DartCorpCodeClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockCorpCodeSyncCommandTest {

    private final StockCorpCodeMappingService service = mock(StockCorpCodeMappingService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    @Test
    @DisplayName("[성공] - 매핑을 한 번 실행한 뒤 실패 사유와 PostgreSQL 저장 건수를 출력한다")
    void givenMappingResult_whenExecute_thenPrintDatabaseCount() throws IOException {
        // Given
        String countSql = "SELECT COUNT(*) FROM stocks WHERE corp_code IS NOT NULL";
        when(service.synchronize()).thenReturn(new MappingResult(2, 1, 3,
                List.of(new MappingFailure("123456", FailureReason.STOCK_CODE_NOT_FOUND))));
        when(jdbc.queryForObject(countSql, Long.class)).thenReturn(120L);

        var bytes = new ByteArrayOutputStream();
        try (var output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            // When
            StockCorpCodeSyncCommand.execute(service, jdbc, output);
        }

        // Then
        var order = inOrder(service, jdbc);
        order.verify(service).synchronize();
        order.verify(jdbc).queryForObject(countSql, Long.class);
        order.verifyNoMoreInteractions();

        assertThat(bytes.toString(StandardCharsets.UTF_8)).contains(
                "매핑 성공 6건 (신규 2건, 변경 1건, 동일 3건), 실패 1건",
                "실패 사유 STOCK_CODE_NOT_FOUND 1건",
                "이번 실행 DB 쓰기 3건",
                "PostgreSQL 전체 저장 120건");
    }

    @Test
    @DisplayName("[실패] - 매핑에 실패하면 PostgreSQL 저장 건수를 조회하지 않는다")
    void givenMappingFailure_whenExecute_thenPropagateFailureWithoutCount() throws IOException {
        // Given
        when(service.synchronize()).thenThrow(new IOException("Open DART 호출 실패"));

        var bytes = new ByteArrayOutputStream();
        try (var output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            // When
            assertThatThrownBy(() -> StockCorpCodeSyncCommand.execute(service, jdbc, output))
                    // Then
                    .isInstanceOf(IOException.class)
                    .hasMessage("Open DART 호출 실패");
        }

        verifyNoInteractions(jdbc);
        assertThat(bytes.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DB_URL", matches = "jdbc:postgresql:.*")
    @EnabledIfEnvironmentVariable(named = "DB_USER_NAME", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".*")
    @DisplayName("[성공] - 전용 실행 환경에서 ZIP을 처리하고 PostgreSQL 저장 건수를 조회한다")
    void givenCorpCodeArchive_whenExecute_thenReadCommittedCount(@TempDir Path directory)
            throws IOException, SQLException {
        // Given
        String schema = "dart_sync_" + UUID.randomUUID().toString().replace("-", "");

        try (var connection = DriverManager.getConnection(
                System.getenv("DB_URL"), System.getenv("DB_USER_NAME"), System.getenv("DB_PASSWORD"));
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);

            try (var context = StockCorpCodeSyncCommand.application()
                    .sources(DartFixture.class)
                    .run("--spring.datasource.url=${DB_URL}",
                            "--spring.datasource.username=${DB_USER_NAME}",
                            "--spring.datasource.password=${DB_PASSWORD}",
                            "--spring.datasource.hikari.schema=" + schema,
                            "--spring.jpa.properties.hibernate.default_schema=" + schema,
                            "--spring.jpa.hibernate.ddl-auto=create-drop",
                            "--OPEN_DART_CORP_CODE_DOWNLOAD_DIR=" + directory)) {
                var repository = context.getBean(StockRepository.class);
                repository.save(Stock.createStock(null, null, "005930", "삼성전자", "삼성전자",
                        DomesticStockMarketType.KOSPI, null, null, null, null, null, true, false));

                var bytes = new ByteArrayOutputStream();
                try (var output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                    // When
                    StockCorpCodeSyncCommand.execute(context.getBean(StockCorpCodeMappingService.class),
                            context.getBean(JdbcTemplate.class), output);
                }

                // Then
                verify(context.getBean(DartCorpCodeClient.class)).fetchArchive();
                assertThat(repository.findByStockCode("005930").orElseThrow().getCorpCode()).isEqualTo("00126380");
                assertThat(bytes.toString(StandardCharsets.UTF_8))
                        .contains("이번 실행 DB 쓰기 1건", "PostgreSQL 전체 저장 1건");
                assertThat(context.getBeansOfType(org.springframework.batch.core.Job.class)).isEmpty();
                assertThat(context.getBeansOfType(
                        org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.class)).isEmpty();
            } finally {
                statement.execute("DROP SCHEMA " + schema);
            }
        }
    }

    static class DartFixture {
        @Bean
        @Primary
        DartCorpCodeClient dartFixtureClient() throws IOException {
            var bytes = new ByteArrayOutputStream();

            try (var zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry("CORPCODE.xml"));
                zip.write("""
                        <result><list><corp_code>00126380</corp_code><stock_code>005930</stock_code></list></result>
                        """.getBytes(StandardCharsets.UTF_8));
            }
            var client = mock(DartCorpCodeClient.class);
            when(client.fetchArchive()).thenReturn(bytes.toByteArray());

            return client;
        }
    }
}
