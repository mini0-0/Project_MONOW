package com.monow.external.dart.corpcode;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;

@Component
public class DartCorpCodeExtractor {

    private static final int MAX_XML_BYTES = 64 * 1024 * 1024;

    public byte[] extract(Path archive) throws IOException {
        try (var zip = new ZipFile(archive.toFile())) {
            var entry = zip.stream()
                    .filter(candidate -> !candidate.isDirectory() && "CORPCODE.xml".equalsIgnoreCase(candidate.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Open DART ZIP에 CORPCODE.xml이 없습니다."));

            try (var input = zip.getInputStream(entry)) {
                byte[] xml = input.readNBytes(MAX_XML_BYTES + 1);

                if (xml.length > MAX_XML_BYTES) {
                    throw new IOException("Open DART 고유번호 XML 크기 제한을 초과했습니다.");
                }

                var crc = new CRC32();
                crc.update(xml);

                if (xml.length != entry.getSize() || crc.getValue() != entry.getCrc()) {
                    throw new IOException("Open DART 고유번호 XML의 크기 또는 CRC가 일치하지 않습니다.");
                }
                return xml;
            }
        }
    }
}
