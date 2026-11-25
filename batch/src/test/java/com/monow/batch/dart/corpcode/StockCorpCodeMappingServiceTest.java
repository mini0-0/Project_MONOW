package com.monow.batch.dart.corpcode;

import com.monow.batch.dart.corpcode.StockCorpCodeMappingService.FailureReason;
import com.monow.batch.dart.corpcode.StockCorpCodeMappingService.MappingFailure;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.domain.stock.repository.StockRepository.CorpCodeMappingTarget;
import com.monow.external.dart.corpcode.DartCorpCodeDownloader;
import com.monow.external.dart.corpcode.DartCorpCodeExtractor;
import com.monow.external.dart.corpcode.DartCorpCodeParser;
import com.monow.external.dart.corpcode.DartCorpCodeParser.CorpCodeEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StockCorpCodeMappingServiceTest {

    private final DartCorpCodeDownloader downloader = mock(DartCorpCodeDownloader.class);
    private final DartCorpCodeExtractor extractor = mock(DartCorpCodeExtractor.class);
    private final DartCorpCodeParser parser = mock(DartCorpCodeParser.class);
    private final StockRepository repository = mock(StockRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final StockCorpCodeMappingService service =
            new StockCorpCodeMappingService(downloader, extractor, parser, repository, transactionManager);

    @Test
    @DisplayName("[성공] - 공통 기업코드를 저장하고 기존 값과 미매핑 원인을 집계한다")
    void givenCorpCodesAndStocks_whenSynchronize_thenUpdateOnlyChangedMatches() throws IOException {
        // Given
        var transaction = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transaction);
        Path archive = Path.of("corpCode.zip");
        byte[] xml = new byte[0];
        when(downloader.download()).thenReturn(archive);
        when(extractor.extract(archive)).thenReturn(xml);
        when(parser.parse(xml)).thenReturn(List.of(
                new CorpCodeEntry("005930", "00126380"),
                new CorpCodeEntry("005935", "00126380"),
                new CorpCodeEntry("000660", "00164779"),
                new CorpCodeEntry("000660", "00164779"),
                new CorpCodeEntry("005380", "00164742"),
                new CorpCodeEntry("111111", "00000001"),
                new CorpCodeEntry("111111", "00000002"),
                new CorpCodeEntry("222222", "bad"),
                new CorpCodeEntry("333333", "00000003"),
                new CorpCodeEntry("", "00000004")));
        when(repository.findCorpCodeMappingTargets()).thenReturn(List.of(
                target("005930", null), target("005935", null), target("000660", "00164779"),
                target("005380", "00000001"), target("555555", "00000005"), target("111111", null),
                target("222222", null), target("333333", null)));
        when(repository.updateCorpCode("005930", "00126380", null, true)).thenReturn(1);
        when(repository.updateCorpCode("005935", "00126380", null, true)).thenReturn(1);
        when(repository.updateCorpCode("005380", "00164742", "00000001", false)).thenReturn(1);

        // When
        var result = service.synchronize();

        // Then
        assertThat(result.successCount()).isEqualTo(4);
        assertThat(result.assigned()).isEqualTo(2);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.unchanged()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(4);
        assertThat(result.failures()).containsExactly(
                new MappingFailure("555555", FailureReason.STOCK_CODE_NOT_FOUND),
                new MappingFailure("111111", FailureReason.CONFLICTING_CORP_CODES),
                new MappingFailure("222222", FailureReason.INVALID_CORP_CODE),
                new MappingFailure("333333", FailureReason.CONCURRENT_CHANGE));

        verify(downloader).download();
        verify(extractor).extract(archive);
        verify(parser).parse(xml);
        verify(repository).findCorpCodeMappingTargets();
        verify(repository).updateCorpCode("005930", "00126380", null, true);
        verify(repository).updateCorpCode("005935", "00126380", null, true);
        verify(repository).updateCorpCode("005380", "00164742", "00000001", false);
        verify(repository).updateCorpCode("333333", "00000003", null, true);
        verifyNoMoreInteractions(repository);
        verify(transactionManager).commit(transaction);
    }

    @Test
    @DisplayName("[실패] - 고유번호 목록을 받지 못하면 DB에 접근하지 않는다")
    void givenDartFailure_whenSynchronize_thenDoNotStartTransaction() throws IOException {
        // Given
        when(downloader.download()).thenThrow(new IOException("Open DART 오류"));

        // When
        assertThatThrownBy(service::synchronize).isInstanceOf(IOException.class);
        // Then
        verifyNoInteractions(extractor, parser, repository, transactionManager);
    }

    private static CorpCodeMappingTarget target(String stockCode, String corpCode) {
        return new CorpCodeMappingTarget() {
            @Override
            public String getStockCode() {
                return stockCode;
            }

            @Override
            public String getCorpCode() {
                return corpCode;
            }
        };
    }
}
