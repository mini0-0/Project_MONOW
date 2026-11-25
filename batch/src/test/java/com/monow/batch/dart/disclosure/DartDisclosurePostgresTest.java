package com.monow.batch.dart.disclosure;

import com.monow.batch.dart.DartCommandApplication;
import com.monow.batch.dart.document.DartDocumentDownloadCommand;
import com.monow.batch.dart.document.DartDocumentDownloadService;
import com.monow.domain.dart.repository.DartDisclosureRepository;
import com.monow.domain.stock.entity.DomesticStockMarketType;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.dart.disclosure.DartDisclosureClient;
import com.monow.external.dart.disclosure.dto.response.DartDisclosureItem;
import com.monow.external.dart.document.DartDocumentClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "DB_URL", matches = "jdbc:postgresql:.*")
@EnabledIfEnvironmentVariable(named = "DB_USER_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".*")
class DartDisclosurePostgresTest {

    private static final String RECEIPT = "20261001000001";
    private static final String EXISTING_RECEIPT = "20261001000002";
    private static final String SECOND_RECEIPT = "20261001000003";
    private static final String ROLLED_BACK_RECEIPT = "20261001000004";
    private static final LocalDate DATE = LocalDate.of(2026, 10, 1);
    private static final LocalDateTime EXISTING_CREATED_AT = DATE.atTime(12, 0);
    private static final LocalDateTime EXISTING_UPDATED_AT = DATE.atTime(13, 0);

    @ParameterizedTest(name = "기존 테이블 전환 여부 {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("[성공] - 중복 기업을 제외하고 실패 기업만 롤백하며 재수집 시 기존 행을 유지한다")
    void givenDisclosureSchema_whenCollectTwiceAndDownload_thenKeepRowsAndSaveZip(
            boolean migrateExistingTable, @TempDir Path directory
    ) throws IOException, InterruptedException {
        // Given
        var dataSource = new DriverManagerDataSource(System.getenv("DB_URL"),
                System.getenv("DB_USER_NAME"), System.getenv("DB_PASSWORD"));
        var jdbc = new JdbcTemplate(dataSource);
        String schema = "dart_disclosure_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE SCHEMA " + schema);
        String poolSchema = "--spring.datasource.hikari.schema=" + schema;
        String jpaSchema = "--spring.jpa.properties.hibernate.default_schema=" + schema;

        try {
            try (var context = DartCommandApplication
                    .application(DartDisclosureCollectionCommand.CollectionConfiguration.class)
                    .sources(Fixture.class).run(poolSchema, jpaSchema, "--spring.jpa.hibernate.ddl-auto=create")) {
                var stocks = context.getBean(StockRepository.class);
                for (String stockCode : List.of("005930", "005935", "000660", "000661", "000662")) {
                    stocks.save(Stock.createStock(null, null, stockCode, stockCode,
                            stockCode, DomesticStockMarketType.KOSPI, null, null, null, null, null, true, false));
                }
                var schemaJdbc = new JdbcTemplate(context.getBean(DataSource.class));
                schemaJdbc.update("UPDATE stocks SET corp_code = '00126380' WHERE stock_code IN ('005930', '005935')");
                schemaJdbc.update("UPDATE stocks SET corp_code = '00164779' WHERE stock_code = '000660'");
                schemaJdbc.update("UPDATE stocks SET corp_code = '00130000' WHERE stock_code = '000661'");

                if (migrateExistingTable) {
                    schemaJdbc.execute("DROP TABLE dart_disclosures");
                    schemaJdbc.execute("""
                            CREATE TABLE dart_disclosures (
                                rcept_no VARCHAR(14) PRIMARY KEY,
                                corp_code VARCHAR(8) NOT NULL,
                                report_nm TEXT NOT NULL,
                                rcept_dt DATE NOT NULL,
                                flr_nm TEXT,
                                created_at TIMESTAMP,
                                updated_at TIMESTAMP
                            )
                            """);
                    schemaJdbc.update("""
                            INSERT INTO dart_disclosures
                                (rcept_no, corp_code, report_nm, rcept_dt, flr_nm, created_at, updated_at)
                            VALUES (?, '00126380', '기존 공시', ?, '기존 제출인', ?, ?)
                            """, EXISTING_RECEIPT, DATE, EXISTING_CREATED_AT, EXISTING_UPDATED_AT);
                    new ResourceDatabasePopulator(new ClassPathResource("db/dart_disclosures_add_id.sql"))
                            .execute(schemaJdbc.getDataSource());
                }
            }

            try (var context = DartCommandApplication
                    .application(DartDisclosureCollectionCommand.CollectionConfiguration.class)
                    .sources(Fixture.class).run(poolSchema, jpaSchema)) {
                var service = context.getBean(DartDisclosureCollectionService.class);
                var repository = context.getBean(DartDisclosureRepository.class);
                assertThat(context.getBean(StockRepository.class).findDistinctCorpCodes())
                        .containsExactly("00126380", "00130000", "00164779");
                // When
                var first = service.collectAll(DATE, DATE);
                var original = repository.findByReceiptNumber(RECEIPT).orElseThrow();
                var second = service.collectAll(DATE, DATE);

                // Then
                assertThat(first.targetCompanies()).isEqualTo(3);
                assertThat(first.successfulCompanies()).isEqualTo(2);
                assertThat(first.failedCompanies()).isEqualTo(1);
                assertThat(first.failures()).extracting(DartDisclosureCollectionService.CollectionFailure::corpCode)
                        .containsExactly("00130000");
                assertThat(first.inserted()).isEqualTo(2);
                assertThat(second.inserted()).isZero();
                assertThat(second.duplicates()).isEqualTo(2);
                assertThat(repository.existsByReceiptNumber(SECOND_RECEIPT)).isTrue();
                assertThat(repository.existsByReceiptNumber(ROLLED_BACK_RECEIPT)).isFalse();
                assertThat(original.getId()).isPositive();

                var stored = repository.findById(original.getId()).orElseThrow();

                assertThat(stored.getReceiptNumber()).isEqualTo(RECEIPT);
                assertThat(stored.getCreatedAt()).isEqualTo(original.getCreatedAt());
                assertThat(stored.getUpdatedAt()).isEqualTo(original.getUpdatedAt());

                assertThat(stored.getCreatedAt()).isNotNull();
                assertThat(stored.getUpdatedAt()).isNotNull();

                if (migrateExistingTable) {
                    var existing = repository.findByReceiptNumber(EXISTING_RECEIPT).orElseThrow();
                    assertThat(existing.getId()).isPositive().isNotEqualTo(original.getId());
                    assertThat(existing.getReportName()).isEqualTo("기존 공시");
                    assertThat(existing.getFilerName()).isEqualTo("기존 제출인");
                    assertThat(existing.getCreatedAt()).isEqualTo(EXISTING_CREATED_AT);
                    assertThat(existing.getUpdatedAt()).isEqualTo(EXISTING_UPDATED_AT);
                }

            }

            try (var context = DartCommandApplication
                    .application(DartDocumentDownloadCommand.DownloadConfiguration.class)
                    .sources(Fixture.class).run(poolSchema, jpaSchema, "--OPEN_DART_DOWNLOAD_DIR=" + directory)) {
                // When
                var path = context.getBean(DartDocumentDownloadService.class).download(RECEIPT);
                // Then
                assertThat(path).isRegularFile().hasFileName(RECEIPT + ".zip");
            }
        } finally {
            jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    static class Fixture {

        @Bean
        @Primary
        DartDisclosureClient listFixture() throws IOException {
            var client = mock(DartDisclosureClient.class);
            when(client.fetchDisclosures("00126380", DATE, DATE)).thenReturn(
                    List.of(new DartDisclosureItem(RECEIPT, "00126380", "보고서", "20261001", "제출인")));
            when(client.fetchDisclosures("00130000", DATE, DATE)).thenReturn(List.of(
                    new DartDisclosureItem(ROLLED_BACK_RECEIPT, "00130000", "보고서", "20261001", "제출인"),
                    new DartDisclosureItem("bad-receipt", "00130000", "보고서", "20261001", "제출인")));
            when(client.fetchDisclosures("00164779", DATE, DATE)).thenReturn(
                    List.of(new DartDisclosureItem(SECOND_RECEIPT, "00164779", "보고서", "20261001", "제출인")));
            return client;
        }

        @Bean
        @Primary
        DartDocumentClient documentFixture() throws IOException {
            var bytes = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry("document.xml"));
                zip.write(1);
            }
            var client = mock(DartDocumentClient.class);
            when(client.fetchDocument(RECEIPT)).thenReturn(bytes.toByteArray());
            return client;
        }
    }
}
