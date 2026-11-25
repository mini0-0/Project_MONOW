package com.monow.batch.dart;

import com.monow.domain.dart.entity.DartDisclosure;
import com.monow.domain.dart.repository.DartDisclosureRepository;
import com.monow.domain.stock.entity.Stock;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.global.config.QueryDSLConfig;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Map;

@ImportAutoConfiguration({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EntityScan(basePackageClasses = {Stock.class, DartDisclosure.class})
@EnableJpaRepositories(basePackageClasses = {StockRepository.class, DartDisclosureRepository.class})
@Import(QueryDSLConfig.class)
public class DartCommandApplication {

    public static SpringApplicationBuilder application(Class<?> useCase) {
        return new SpringApplicationBuilder(DartCommandApplication.class, useCase)
                .web(WebApplicationType.NONE)
                .properties(Map.of(
                        "spring.config.name", "monow-dart-disclosure",
                        "spring.datasource.url", "${DB_URL}",
                        "spring.datasource.username", "${DB_USER_NAME}",
                        "spring.datasource.password", "${DB_PASSWORD}",
                        "spring.jpa.hibernate.ddl-auto", "validate"
                ));
    }

    public static String requiredOption(DefaultApplicationArguments arguments, String name) {
        var values = arguments.getOptionValues(name);

        if (values == null || values.size() != 1 || values.getFirst().isBlank()) {
            throw new IllegalArgumentException("--" + name + " 값을 한 번 지정해야 합니다.");
        }

        return values.getFirst();
    }
}
