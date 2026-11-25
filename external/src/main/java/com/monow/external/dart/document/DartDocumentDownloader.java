package com.monow.external.dart.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;

@Component
public class DartDocumentDownloader {

    private static final long MAX_UNCOMPRESSED_BYTES = 200L * 1024 * 1024;

    private final DartDocumentClient client;
    private final Path downloadDirectory;

    public DartDocumentDownloader(
            DartDocumentClient client,
            @Value("${OPEN_DART_DOWNLOAD_DIR:./downloads/dart}") String downloadDirectory
    ) {
        this.client = client;
        this.downloadDirectory = Path.of(downloadDirectory).toAbsolutePath().normalize();
    }

    public Path download(String receiptNumber) throws IOException {
        byte[] body = client.fetchDocument(receiptNumber);
        Files.createDirectories(downloadDirectory);
        Path target = downloadDirectory.resolve(receiptNumber + ".zip");
        Path temporaryFile = Files.createTempFile(downloadDirectory, receiptNumber + "-", ".part");

        try {
            Files.write(temporaryFile, body);
            validateZip(temporaryFile);
            return Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    private static void validateZip(Path file) throws IOException {
        long totalBytes = 0;
        byte[] buffer = new byte[8192];

        try (var zip = new ZipFile(file.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                var crc = new CRC32();
                long entryBytes = 0;
                try (var input = zip.getInputStream(entry)) {
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        totalBytes += count;
                        if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                            throw new IOException("DART ZIP 검증 크기 제한을 초과했습니다.");
                        }
                        entryBytes += count;
                        crc.update(buffer, 0, count);
                    }
                }

                if (entryBytes != entry.getSize() || crc.getValue() != entry.getCrc()) {
                    throw new IOException("DART ZIP 엔트리의 크기 또는 CRC가 일치하지 않습니다.");
                }
            }
        }

        if (totalBytes == 0) {
            throw new IOException("DART ZIP에 내용이 있는 파일이 없습니다.");
        }
    }
}
