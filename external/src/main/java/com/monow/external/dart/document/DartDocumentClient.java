package com.monow.external.dart.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class DartDocumentClient {

    private static final String BASE_URL = "https://opendart.fss.or.kr";
    private static final int MAX_DOWNLOAD_BYTES = 20 * 1024 * 1024;
    private static final int MAX_ERROR_XML_BYTES = 16 * 1024;

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public DartDocumentClient(@Value("${OPEN_DART_API_KEY:}") String apiKey) {
        this(createRestClient(), apiKey);
    }

    DartDocumentClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public byte[] fetchDocument(String receiptNumber) throws IOException {
        if (receiptNumber == null || !receiptNumber.matches("[0-9]{14}")) {
            throw new IllegalArgumentException("DART 접수번호는 숫자 14자리여야 합니다.");
        }
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OPEN_DART_API_KEY가 설정되지 않았습니다.");
        }

        byte[] body;
        try {
            body = restClient.get()
                    .uri(builder -> builder.path("/api/document.xml")
                            .queryParam("crtfc_key", "{apiKey}")
                            .queryParam("rcept_no", "{receiptNumber}")
                            .build(apiKey, receiptNumber))
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new UncheckedIOException(
                                    new IOException("DART HTTP 오류: " + response.getStatusCode().value())
                            );
                        }
                        return response.getBody().readNBytes(MAX_DOWNLOAD_BYTES + 1);
                    });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        } catch (RestClientException exception) {
            // Spring의 전송 예외에는 인증키가 포함된 요청 URL이 들어갈 수 있습니다.
            throw new IOException("DART 응답을 읽지 못했습니다.");
        }

        if (body.length > MAX_DOWNLOAD_BYTES) {
            throw new IOException("DART 응답 크기 제한을 초과했습니다.");
        }
        if (!hasZipSignature(body)) {
            throw new IOException("DART ZIP 응답을 받지 못했습니다. 상태 코드: " + readDartStatus(body));
        }
        return body;
    }

    private static RestClient createRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(30_000);

        return RestClient.builder().baseUrl(BASE_URL).requestFactory(factory).build();
    }

    private static boolean hasZipSignature(byte[] body) {
        return body.length >= 4 && body[0] == 'P' && body[1] == 'K'
                && ((body[2] == 3 && body[3] == 4) || (body[2] == 5 && body[3] == 6));
    }

    private static String readDartStatus(byte[] body) {
        if (body.length > MAX_ERROR_XML_BYTES) {
            return "확인 불가";
        }

        try (var input = new ByteArrayInputStream(body)) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            var statuses = builder.parse(input).getElementsByTagName("status");

            if (statuses.getLength() == 1) {
                String status = statuses.item(0).getTextContent().trim();
                if (status.matches("[0-9]{3}")) {
                    return status;
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException exception) {
            return "확인 불가";
        }
        return "확인 불가";
    }
}
