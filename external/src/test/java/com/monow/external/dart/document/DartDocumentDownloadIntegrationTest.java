package com.monow.external.dart.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "OPEN_DART_API_KEY", matches = "\\S+")
@EnabledIfEnvironmentVariable(named = "OPEN_DART_TEST_RECEIPT_NO", matches = "[0-9]{14}")
class DartDocumentDownloadIntegrationTest {

    @Test
    @DisplayName("[성공] - 실제 DART 공시 한 건을 읽을 수 있는 ZIP으로 저장한다")
    void givenRealReceiptNumber_whenDownload_thenSaveReadableZip() throws IOException {
        // Given
        String receiptNumber = System.getenv("OPEN_DART_TEST_RECEIPT_NO");
        String directory = System.getenv().getOrDefault("OPEN_DART_DOWNLOAD_DIR", "./downloads/dart");
        var client = new DartDocumentClient(System.getenv("OPEN_DART_API_KEY"));
        var downloader = new DartDocumentDownloader(client, directory);

        // When
        Path saved = downloader.download(receiptNumber);

        // Then
        assertThat(saved).isRegularFile().hasFileName(receiptNumber + ".zip");
        try (var zip = new ZipFile(saved.toFile())) {
            var entry = zip.stream()
                    .filter(candidate -> !candidate.isDirectory() && candidate.getSize() > 0)
                    .findFirst()
                    .orElseThrow();

            try (var input = zip.getInputStream(entry)) {
                assertThat(input.read()).isGreaterThanOrEqualTo(0);
            }
        }
    }
}
