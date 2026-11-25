package com.monow.batch.dart.corpcode;

import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.dart.corpcode.DartCorpCodeDownloader;
import com.monow.external.dart.corpcode.DartCorpCodeExtractor;
import com.monow.external.dart.corpcode.DartCorpCodeParser;
import com.monow.external.dart.corpcode.DartCorpCodeParser.CorpCodeEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StockCorpCodeMappingService {

    private final DartCorpCodeDownloader downloader;
    private final DartCorpCodeExtractor extractor;
    private final DartCorpCodeParser parser;
    private final StockRepository stockRepository;
    private final TransactionTemplate transaction;

    public StockCorpCodeMappingService(
            DartCorpCodeDownloader downloader,
            DartCorpCodeExtractor extractor,
            DartCorpCodeParser parser,
            StockRepository stockRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.downloader = downloader;
        this.extractor = extractor;
        this.parser = parser;
        this.stockRepository = stockRepository;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public MappingResult synchronize() throws IOException {
        Path archive = downloader.download();
        byte[] xml = extractor.extract(archive);
        Map<String, Set<String>> codesByStock = new HashMap<>();

        for (CorpCodeEntry entry : parser.parse(xml)) {
            if (entry.stockCode().matches("[A-Z0-9]{6}")) {
                codesByStock.computeIfAbsent(entry.stockCode(), ignored -> new HashSet<>()).add(entry.corpCode());
            }
        }

        return transaction.execute(status -> applyMappings(codesByStock));
    }

    private MappingResult applyMappings(Map<String, Set<String>> codesByStock) {
        int assigned = 0;
        int updated = 0;
        int unchanged = 0;

        List<MappingFailure> failures = new ArrayList<>();

        for (var stock : stockRepository.findCorpCodeMappingTargets()) {
            String stockCode = stock.getStockCode();
            Set<String> candidates = codesByStock.get(stockCode);
            if (candidates == null) {
                failures.add(new MappingFailure(stockCode, FailureReason.STOCK_CODE_NOT_FOUND));
                continue;
            }
            if (candidates.size() > 1) {
                failures.add(new MappingFailure(stockCode, FailureReason.CONFLICTING_CORP_CODES));
                continue;
            }

            String corpCode = candidates.iterator().next();
            String previous = stock.getCorpCode();

            if (!corpCode.matches("[0-9]{8}")) {
                failures.add(new MappingFailure(stockCode, FailureReason.INVALID_CORP_CODE));
            } else if (corpCode.equals(previous)) {
                unchanged++;
            } else if (stockRepository.updateCorpCode(stockCode, corpCode, previous, previous == null) == 0) {
                failures.add(new MappingFailure(stockCode, FailureReason.CONCURRENT_CHANGE));
            } else if (previous == null) {
                assigned++;
            } else {
                updated++;
            }
        }

        return new MappingResult(assigned, updated, unchanged, List.copyOf(failures));
    }

    public record MappingResult(int assigned, int updated, int unchanged, List<MappingFailure> failures) {

        public int successCount() {
            return assigned + updated + unchanged;
        }

        public int failureCount() {
            return failures.size();
        }

        public Map<FailureReason, Integer> failureCounts() {
            var counts = new EnumMap<FailureReason, Integer>(FailureReason.class);
            for (MappingFailure failure : failures) {
                counts.merge(failure.reason(), 1, Integer::sum);
            }

            return Map.copyOf(counts);
        }
    }

    public record MappingFailure(String stockCode, FailureReason reason) {
    }

    public enum FailureReason {
        STOCK_CODE_NOT_FOUND,
        CONFLICTING_CORP_CODES,
        INVALID_CORP_CODE,
        CONCURRENT_CHANGE
    }
}
