package com.monow.external.dart.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DartDocumentDownloaderTest {

    private static final String RECEIPT_NUMBER = "20260102000001";
    private static final String DOCUMENT_NAME = "document.xml";
    private static final byte[] DOCUMENT = "<document>공시 본문</document>".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path directory;

    private DartDocumentClient client;
    private DartDocumentDownloader downloader;

    @BeforeEach
    void setUp() {
        client = mock(DartDocumentClient.class);
        downloader = new DartDocumentDownloader(client, directory.toString());
    }

    @Test
    @DisplayName("[성공] - 검증한 ZIP의 원본 바이트를 접수번호 파일로 저장한다")
    void givenValidZip_whenDownload_thenSaveOriginalBytes() throws IOException {
        // Given
        byte[] archive = createZip(true);
        when(client.fetchDocument(RECEIPT_NUMBER)).thenReturn(archive);

        // When
        Path saved = downloader.download(RECEIPT_NUMBER);

        // Then
        assertThat(saved).isEqualTo(directory.resolve(RECEIPT_NUMBER + ".zip"));
        assertThat(Files.readAllBytes(saved)).containsExactly(archive);
        assertDirectoryContainsOnly(saved);
    }

    @Test
    @DisplayName("[실패] - 잘린 ZIP을 검증하면 임시 파일을 정리한다")
    void givenTruncatedZip_whenDownload_thenCleanUpTemporaryFile() throws IOException {
        // Given
        byte[] archive = createZip(true);
        when(client.fetchDocument(RECEIPT_NUMBER)).thenReturn(Arrays.copyOf(archive, archive.length - 10));

        // When
        assertThatThrownBy(() -> downloader.download(RECEIPT_NUMBER))
                // Then
                .isInstanceOf(IOException.class);

        assertDirectoryContainsOnly();
    }

    @Test
    @DisplayName("[실패] - CRC가 다른 재다운로드 응답을 거부하고 기존 ZIP을 보존한다")
    void givenCorruptReplacement_whenDownload_thenPreservePreviousFile() throws IOException {
        // Given
        byte[] archive = createZip(true);
        byte[] corruptArchive = archive.clone();
        // STORED 엔트리의 본문은 중앙 디렉터리와 CRC를 변경하지 않고 변조할 수 있습니다.
        int bodyOffset = 30 + DOCUMENT_NAME.length();
        corruptArchive[bodyOffset] ^= 1;
        when(client.fetchDocument(RECEIPT_NUMBER)).thenReturn(archive, corruptArchive);
        Path saved = downloader.download(RECEIPT_NUMBER);

        // When
        assertThatThrownBy(() -> downloader.download(RECEIPT_NUMBER))
                // Then
                .isInstanceOf(IOException.class)
                .hasMessageContaining("CRC");

        assertThat(Files.readAllBytes(saved)).containsExactly(archive);
        assertDirectoryContainsOnly(saved);
    }

    @Test
    @DisplayName("[실패] - 파일 엔트리가 없는 ZIP은 저장하지 않는다")
    void givenEmptyZip_whenDownload_thenRejectArchive() throws IOException {
        // Given
        when(client.fetchDocument(RECEIPT_NUMBER)).thenReturn(createZip(false));

        // When
        assertThatThrownBy(() -> downloader.download(RECEIPT_NUMBER))
                // Then
                .isInstanceOf(IOException.class)
                .hasMessageContaining("내용이 있는 파일이 없습니다");

        assertDirectoryContainsOnly();
    }

    private void assertDirectoryContainsOnly(Path... expected) throws IOException {
        try (var files = Files.list(directory)) {
            assertThat(files.toList()).containsExactlyInAnyOrder(expected);
        }
    }

    private static byte[] createZip(boolean includeDocument) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            if (includeDocument) {
                var crc = new CRC32();
                crc.update(DOCUMENT);

                var entry = new ZipEntry(DOCUMENT_NAME);
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(DOCUMENT.length);
                entry.setCrc(crc.getValue());
                zip.putNextEntry(entry);
                zip.write(DOCUMENT);
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
