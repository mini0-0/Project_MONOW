package com.monow.batch.dart.document;

import com.monow.batch.dart.DartCommandApplication;
import com.monow.external.dart.document.DartDocumentClient;
import com.monow.external.dart.document.DartDocumentDownloader;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.util.Set;

public class DartDocumentDownloadCommand {

    public static void main(String[] args) throws IOException {
        var arguments = new DefaultApplicationArguments(args);
        if (!arguments.getNonOptionArgs().isEmpty() || !arguments.getOptionNames().equals(Set.of("receipt-no"))) {
            throw new IllegalArgumentException("--receipt-no 옵션을 지정해야 합니다.");
        }
        String receiptNumber = DartCommandApplication.requiredOption(arguments, "receipt-no");

        try (var context = DartCommandApplication.application(DownloadConfiguration.class).run()) {
            var path = context.getBean(DartDocumentDownloadService.class).download(receiptNumber);
            System.out.printf("공시 원본 ZIP 저장 %s%n", path);
        }
    }

    @Import({DartDocumentClient.class, DartDocumentDownloader.class, DartDocumentDownloadService.class})
    public static class DownloadConfiguration {
    }
}
