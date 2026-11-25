package com.monow.batch.dart.disclosure;

import com.monow.domain.dart.repository.DartDisclosureRepository;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.dart.disclosure.DartDisclosureClient;
import com.monow.external.dart.disclosure.dto.response.DartDisclosureItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DartDisclosureCollectionService {

    private static final Logger log = LoggerFactory.getLogger(DartDisclosureCollectionService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final StockRepository stocks;
    private final DartDisclosureClient client;
    private final DartDisclosureRepository disclosures;
    private final TransactionTemplate transaction;
    private final Duration companyInterval;
    private final Duration retryInterval;

    @Autowired
    public DartDisclosureCollectionService(
            StockRepository stocks, DartDisclosureClient client, DartDisclosureRepository disclosures,
            PlatformTransactionManager transactionManager
    ) {
        this(stocks, client, disclosures, transactionManager, Duration.ofMillis(250), Duration.ofSeconds(2));
    }

    DartDisclosureCollectionService(
            StockRepository stocks, DartDisclosureClient client, DartDisclosureRepository disclosures,
            PlatformTransactionManager transactionManager, Duration companyInterval, Duration retryInterval
    ) {
        this.stocks = stocks;
        this.client = client;
        this.disclosures = disclosures;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.companyInterval = companyInterval;
        this.retryInterval = retryInterval;
    }

    public CollectionResult collectAll(LocalDate from, LocalDate to) throws InterruptedException {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("시작일은 종료일 이하여야 합니다.");
        }

        List<String> corpCodes = stocks.findDistinctCorpCodes();
        List<CollectionFailure> failures = new ArrayList<>();
        long received = 0;
        long inserted = 0;

        for (int index = 0; index < corpCodes.size(); index++) {
            if (index > 0) {
                Thread.sleep(companyInterval);
            }
            String corpCode = corpCodes.get(index);
            try {
                CompanyResult result = collectCompany(corpCode, from, to);
                received += result.received();
                inserted += result.inserted();
            } catch (IOException | IllegalArgumentException exception) {
                recordFailure(failures, corpCode, exception.getMessage());
            } catch (RuntimeException exception) {
                recordFailure(failures, corpCode, exception.getClass().getSimpleName());
            }
        }

        return new CollectionResult(corpCodes.size(), corpCodes.size() - failures.size(),
                received, inserted, received - inserted, List.copyOf(failures));
    }

    private CompanyResult collectCompany(String corpCode, LocalDate from, LocalDate to)
            throws IOException, InterruptedException {
        if (!corpCode.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("DART 기업코드 형식이 유효하지 않습니다.");
        }
        List<DartDisclosureItem> items = fetchWithRetry(corpCode, from, to);

        Integer inserted = transaction.execute(
                status -> save(items, corpCode, from, to)
        );

        return new CompanyResult(items.size(), inserted);
    }

    private List<DartDisclosureItem> fetchWithRetry(String corpCode, LocalDate from, LocalDate to)
            throws IOException, InterruptedException {

        for (int attempt = 1; ; attempt++) {
            try {
                return client.fetchDisclosures(corpCode, from, to);
            } catch (IOException exception) {
                if (attempt == MAX_ATTEMPTS || !isRetryable(exception)) {
                    throw exception;
                }
                Thread.sleep(retryInterval.multipliedBy(attempt));
            }
        }
    }

    private static boolean isRetryable(IOException exception) {
        String message = exception.getMessage();

        return message != null && (message.startsWith("Open DART 공시 목록 요청에 실패했습니다.")
                || message.equals("Open DART 공시 목록 오류: 020")
                || message.equals("Open DART 공시 목록 오류: 800")
                || message.equals("Open DART 공시 목록 오류: 900"));
    }

    private static void recordFailure(List<CollectionFailure> failures, String corpCode, String reason) {
        failures.add(new CollectionFailure(corpCode, reason));
        log.warn("Open DART 공시 수집 실패. 기업코드={}, 원인={}", corpCode, reason);
    }

    private int save(List<DartDisclosureItem> items, String corpCode, LocalDate from, LocalDate to) {
        int inserted = 0;

        for (DartDisclosureItem item : items) {
            if (!corpCode.equals(item.corpCode()) || item.receiptNumber() == null
                    || !item.receiptNumber().matches("[0-9]{14}") || item.reportName() == null
                    || item.reportName().isBlank() || item.receiptDate() == null) {
                throw new IllegalArgumentException("Open DART 공시 목록의 필수 필드가 유효하지 않습니다.");
            }

            LocalDate receiptDate;

            try {
                receiptDate = LocalDate.parse(item.receiptDate(), DateTimeFormatter.BASIC_ISO_DATE);
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("Open DART 공시 접수일이 유효하지 않습니다.", exception);
            }
            if (receiptDate.isBefore(from) || receiptDate.isAfter(to)) {
                throw new IllegalArgumentException("Open DART 공시 접수일이 조회 기간을 벗어났습니다.");
            }

            inserted += disclosures.insertIfAbsent(item.receiptNumber(), corpCode, item.reportName(),
                    receiptDate, item.filerName());
        }

        return inserted;
    }

    public record CollectionResult(
            int targetCompanies, int successfulCompanies, long received, long inserted, long duplicates,
            List<CollectionFailure> failures
    ) {
        public int failedCompanies() {
            return failures.size();
        }
    }

    public record CollectionFailure(String corpCode, String reason) {
    }

    private record CompanyResult(int received, int inserted) {
    }
}
