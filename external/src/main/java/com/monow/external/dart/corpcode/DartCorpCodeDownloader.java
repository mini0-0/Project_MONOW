package com.monow.external.dart.corpcode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipFile;

@Component
public class DartCorpCodeDownloader {

    private final DartCorpCodeClient client;
    private final Path directory;

    public DartCorpCodeDownloader(
            DartCorpCodeClient client,
            @Value("${OPEN_DART_CORP_CODE_DOWNLOAD_DIR:./downloads/dart/corpcode}") String directory
    ) {
        this.client = client;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public Path download() throws IOException {
        byte[] body = client.fetchArchive();
        Files.createDirectories(directory);
        Path temporaryFile = Files.createTempFile(directory, "corpCode-", ".part");

        try {
            Files.write(temporaryFile, body);
            validateArchive(temporaryFile);

            String fileName = temporaryFile.getFileName().toString().replaceFirst("\\.part$", ".zip");

            return Files.move(temporaryFile, directory.resolve(fileName), StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    private static void validateArchive(Path path) throws IOException {
        try (var zip = new ZipFile(path.toFile())) {
            if (zip.size() != 1) {
                throw new IOException("Open DART 고유번호 ZIP에 XML 파일이 정확히 하나 있어야 합니다.");
            }
            var entry = zip.entries().nextElement();
            if (entry.isDirectory() || !"CORPCODE.xml".equalsIgnoreCase(entry.getName())) {
                throw new IOException("Open DART ZIP에 CORPCODE.xml이 없습니다.");
            }
        }
    }
}
