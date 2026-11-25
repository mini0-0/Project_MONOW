package com.monow.batch.dart.disclosure;

import com.monow.batch.dart.DartCommandApplication;
import com.monow.external.dart.disclosure.DartDisclosureClient;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Set;

public class DartDisclosureCollectionCommand {

    public static void main(String[] args) throws InterruptedException {
        var arguments = new DefaultApplicationArguments(args);

        if (!arguments.getNonOptionArgs().isEmpty()
                || !arguments.getOptionNames().equals(Set.of("start-date", "end-date"))) {
            throw new IllegalArgumentException("--start-date, --end-date 옵션을 지정해야 합니다.");
        }

        LocalDate from = LocalDate.parse(DartCommandApplication.requiredOption(arguments, "start-date"));
        LocalDate to = LocalDate.parse(DartCommandApplication.requiredOption(arguments, "end-date"));

        try (var context = DartCommandApplication.application(CollectionConfiguration.class).run()) {
            var result = context.getBean(DartDisclosureCollectionService.class).collectAll(from, to);
            System.out.printf("대상 기업 %d개, 성공 %d개, 실패 %d개%n",
                    result.targetCompanies(), result.successfulCompanies(), result.failedCompanies());
            System.out.printf("공시 조회 %d건, 신규 저장 %d건, 중복 %d건%n",
                    result.received(), result.inserted(), result.duplicates());
        }
    }

    @Import({DartDisclosureClient.class, DartDisclosureCollectionService.class})
    static class CollectionConfiguration {
    }
}
