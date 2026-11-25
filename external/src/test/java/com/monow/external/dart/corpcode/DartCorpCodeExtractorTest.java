package com.monow.external.dart.corpcode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DartCorpCodeExtractorTest {

    private static final String XML_NAME = "CORPCODE.xml";
    private static final byte[] XML = "<result/>".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path directory;

    private final DartCorpCodeExtractor extractor = new DartCorpCodeExtractor();

    @Test
    @DisplayName("[성공] - ZIP의 XML 엔트리를 원본 바이트로 추출한다")
    void givenArchive_whenExtract_thenReturnXmlBytes() throws IOException {
        // Given
        Path archive = createArchive();

        // When
        assertThat(extractor.extract(archive))
                // Then
                .containsExactly(XML);
    }

    @Test
    @DisplayName("[실패] - XML 엔트리의 CRC가 다르면 추출에 실패한다")
    void givenCorruptEntry_whenExtract_thenRejectCrcMismatch() throws IOException {
        // Given
        Path archive = createArchive();
        byte[] bytes = Files.readAllBytes(archive);
        // STORED 엔트리의 로컬 헤더와 파일명 뒤에 원본 XML이 위치합니다.
        bytes[30 + XML_NAME.length()] ^= 1;
        Files.write(archive, bytes);

        // When
        assertThatThrownBy(() -> extractor.extract(archive))
                // Then
                .isInstanceOf(IOException.class)
                .hasMessageContaining("CRC");
    }

    private Path createArchive() throws IOException {
        Path archive = directory.resolve("corpCode.zip");
        var crc = new CRC32();
        crc.update(XML);
        var entry = new ZipEntry(XML_NAME);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(XML.length);
        entry.setCrc(crc.getValue());

        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(entry);
            zip.write(XML);
        }
        return archive;
    }
}
