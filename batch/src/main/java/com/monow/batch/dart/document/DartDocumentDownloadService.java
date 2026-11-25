package com.monow.batch.dart.document;

import com.monow.domain.dart.repository.DartDisclosureRepository;
import com.monow.external.dart.document.DartDocumentDownloader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class DartDocumentDownloadService {

    private final DartDisclosureRepository disclosures;
    private final DartDocumentDownloader downloader;

    public DartDocumentDownloadService(DartDisclosureRepository disclosures, DartDocumentDownloader downloader) {
        this.disclosures = disclosures;
        this.downloader = downloader;
    }

    public Path download(String receiptNumber) throws IOException {
        if (receiptNumber == null || !receiptNumber.matches("[0-9]{14}")) {
            throw new IllegalArgumentException("DART 접수번호는 숫자 14자리여야 합니다.");
        }
        if (!disclosures.existsByReceiptNumber(receiptNumber)) {
            throw new IllegalArgumentException("저장된 DART 공시가 없습니다: " + receiptNumber);
        }

        return downloader.download(receiptNumber);
    }
}
