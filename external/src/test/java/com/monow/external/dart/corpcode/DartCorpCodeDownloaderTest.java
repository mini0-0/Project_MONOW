package com.monow.external.dart.corpcode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DartCorpCodeDownloaderTest {

    @TempDir
    Path directory;

    private final DartCorpCodeClient client = mock(DartCorpCodeClient.class);

    @Test
    @DisplayName("[성공] - 각 다운로드의 ZIP을 별도 경로에 저장한다")
    void givenValidArchive_whenDownloadTwice_thenSaveBothCopies() throws IOException {
        // Given
        var bytes = new ByteArrayOutputStream();

        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("CORPCODE.xml"));
            zip.write(1);
        }

        byte[] archive = bytes.toByteArray();
        when(client.fetchArchive()).thenReturn(archive);
        var downloader = new DartCorpCodeDownloader(client, directory.toString());

        // When
        Path first = downloader.download();
        Path second = downloader.download();

        // Then
        assertThat(first).isNotEqualTo(second);
        assertThat(first.getFileName().toString()).endsWith(".zip");
        assertThat(Files.readAllBytes(first)).containsExactly(archive);
        assertThat(Files.readAllBytes(second)).containsExactly(archive);
    }

    @Test
    @DisplayName("[실패] - 손상된 ZIP을 거부하고 임시 파일을 정리한다")
    void givenInvalidArchive_whenDownload_thenLeaveNoFile() throws IOException {
        // Given
        when(client.fetchArchive()).thenReturn(new byte[] {'P', 'K', 3, 4});
        var downloader = new DartCorpCodeDownloader(client, directory.toString());

        // When
        assertThatThrownBy(downloader::download)
                // Then
                .isInstanceOf(IOException.class);

        try (var files = Files.list(directory)) {
            assertThat(files.toList()).isEmpty();
        }
    }
}
