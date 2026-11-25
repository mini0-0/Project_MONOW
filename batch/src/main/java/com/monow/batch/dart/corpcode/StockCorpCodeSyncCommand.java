package com.monow.batch.dart.corpcode;

import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.dart.corpcode.DartCorpCodeClient;
import com.monow.external.dart.corpcode.DartCorpCodeDownloader;
import com.monow.external.dart.corpcode.DartCorpCodeExtractor;
import com.monow.external.dart.corpcode.DartCorpCodeParser;
import com.monow.global.config.QueryDSLConfig;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

public class StockCorpCodeSyncCommand {

    public static void main(String[] args) throws IOException {
        try (var context = application().run(args)) {
            execute(context.getBean(StockCorpCodeMappingService.class),
                    context.getBean(JdbcTemplate.class), System.out);
        }
    }

    static SpringApplicationBuilder application() {
        return new SpringApplicationBuilder(SyncConfiguration.class)
                .web(WebApplicationType.NONE)
                .properties(Map.of(
                        "spring.config.name", "monow-dart-sync",
                        "spring.datasource.url", "${DB_URL}",
                        "spring.datasource.username", "${DB_USER_NAME}",
                        "spring.datasource.password", "${DB_PASSWORD}",
                        "spring.jpa.hibernate.ddl-auto", "validate"
                ));
    }

    static void execute(
            StockCorpCodeMappingService service,
            JdbcTemplate jdbc,
            PrintStream output
    ) throws IOException {
        var result = service.synchronize();

        long storedCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM stocks WHERE corp_code IS NOT NULL", Long.class);

        output.printf(
                "매핑 성공 %d건 (신규 %d건, 변경 %d건, 동일 %d건), 실패 %d건%n",
                result.successCount(), result.assigned(), result.updated(),
                result.unchanged(), result.failureCount()
        );
        result.failureCounts().forEach((reason, count) ->
                output.printf("실패 사유 %s %d건%n", reason, count));
        output.printf("이번 실행 DB 쓰기 %d건%n", result.assigned() + result.updated());
        output.printf("PostgreSQL 전체 저장 %d건%n", storedCount);
    }

    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class
    })
    @EntityScan(basePackageClasses = Stock.class)
    @EnableJpaRepositories(basePackageClasses = StockRepository.class)
    @Import({
            QueryDSLConfig.class,
            DartCorpCodeClient.class,
            DartCorpCodeDownloader.class,
            DartCorpCodeExtractor.class,
            DartCorpCodeParser.class,
            StockCorpCodeMappingService.class
    })
    static class SyncConfiguration {
    }
}
