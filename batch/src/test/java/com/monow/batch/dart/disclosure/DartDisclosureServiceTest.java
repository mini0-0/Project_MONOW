package com.monow.batch.dart.disclosure;

import com.monow.batch.dart.document.DartDocumentDownloadService;
import com.monow.domain.dart.repository.DartDisclosureRepository;
import com.monow.domain.stock.repository.StockRepository;
import com.monow.external.dart.disclosure.DartDisclosureClient;
import com.monow.external.dart.document.DartDocumentDownloader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DartDisclosureServiceTest {

    private static final String RECEIPT_NUMBER = "20261001000001";

    @Test
    @DisplayName("[실패] - 목록 수집 실패 시 저장 트랜잭션을 시작하지 않는다")
    void givenDartFailure_whenCollect_thenDoNotWrite() throws IOException, InterruptedException {
        // Given
        var stocks = mock(StockRepository.class);
        var client = mock(DartDisclosureClient.class);
        var disclosures = mock(DartDisclosureRepository.class);
        var transactions = mock(PlatformTransactionManager.class);
        var date = LocalDate.of(2026, 10, 1);
        when(stocks.findDistinctCorpCodes()).thenReturn(List.of("00126380"));
        when(client.fetchDisclosures("00126380", date, date))
                .thenThrow(new IOException("Open DART 공시 목록 오류: 010"));
        var service = new DartDisclosureCollectionService(
                stocks, client, disclosures, transactions, Duration.ZERO, Duration.ZERO);

        // When
        var result = service.collectAll(date, date);

        // Then
        assertThat(result.failedCompanies()).isEqualTo(1);
        assertThat(result.failures()).containsExactly(
                new DartDisclosureCollectionService.CollectionFailure("00126380", "Open DART 공시 목록 오류: 010"));
        verify(client).fetchDisclosures("00126380", date, date);
        verifyNoInteractions(disclosures, transactions);
    }

    @Test
    @DisplayName("[성공] - 호출 제한 응답은 정해진 횟수만 재시도하고 다음 기업을 처리한다")
    void givenRepeatedRateLimit_whenCollect_thenContinueWithNextCompany() throws IOException, InterruptedException {
        // Given
        var stocks = mock(StockRepository.class);
        var client = mock(DartDisclosureClient.class);
        var disclosures = mock(DartDisclosureRepository.class);
        var transactions = mock(PlatformTransactionManager.class);
        var transaction = new SimpleTransactionStatus();
        var date = LocalDate.of(2026, 10, 1);
        when(stocks.findDistinctCorpCodes()).thenReturn(List.of("00126380", "00164779"));
        when(client.fetchDisclosures("00126380", date, date))
                .thenThrow(new IOException("Open DART 공시 목록 오류: 020"));
        when(client.fetchDisclosures("00164779", date, date)).thenReturn(List.of());
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(transaction);
        var service = new DartDisclosureCollectionService(
                stocks, client, disclosures, transactions, Duration.ZERO, Duration.ZERO);

        // When
        var result = service.collectAll(date, date);

        // Then
        assertThat(result.targetCompanies()).isEqualTo(2);
        assertThat(result.successfulCompanies()).isEqualTo(1);
        assertThat(result.failedCompanies()).isEqualTo(1);
        verify(client, times(3)).fetchDisclosures("00126380", date, date);
        verify(client).fetchDisclosures("00164779", date, date);
        verify(transactions).commit(transaction);
    }

    @Test
    @DisplayName("[성공] - 저장된 접수번호를 기존 다운로더에 전달한다")
    void givenStoredDisclosure_whenDownload_thenReturnZipPath() throws IOException {
        // Given
        var disclosures = mock(DartDisclosureRepository.class);
        var downloader = mock(DartDocumentDownloader.class);
        Path path = Path.of(RECEIPT_NUMBER + ".zip");
        when(disclosures.existsByReceiptNumber(RECEIPT_NUMBER)).thenReturn(true);
        when(downloader.download(RECEIPT_NUMBER)).thenReturn(path);

        // When
        assertThat(new DartDocumentDownloadService(disclosures, downloader).download(RECEIPT_NUMBER))
                // Then
                .isEqualTo(path);
    }

    @Test
    @DisplayName("[실패] - 저장되지 않은 접수번호로 외부 호출을 하지 않는다")
    void givenUnknownDisclosure_whenDownload_thenSkipDownloader() {
        // Given
        var disclosures = mock(DartDisclosureRepository.class);
        var downloader = mock(DartDocumentDownloader.class);
        var service = new DartDocumentDownloadService(disclosures, downloader);

        // When
        assertThatThrownBy(() -> service.download(RECEIPT_NUMBER)).isInstanceOf(IllegalArgumentException.class);
        // Then
        verifyNoInteractions(downloader);
    }
}
