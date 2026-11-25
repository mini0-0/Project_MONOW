package com.monow.external.dart.corpcode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

@Component
public class DartCorpCodeClient {

    private static final int MAX_ZIP_BYTES = 20 * 1024 * 1024;

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public DartCorpCodeClient(@Value("${OPEN_DART_API_KEY:}") String apiKey) {
        this(createRestClient(), apiKey);
    }

    DartCorpCodeClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public byte[] fetchArchive() throws IOException {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OPEN_DART_API_KEY가 설정되지 않았습니다.");
        }

        ArchiveResponse response;

        try {
            response = restClient.get()
                    .uri(builder -> builder.path("/api/corpCode.xml")
                            .queryParam("crtfc_key", "{apiKey}")
                            .build(apiKey))
                    .exchange((request, httpResponse) -> new ArchiveResponse(
                            httpResponse.getStatusCode().value(),
                            httpResponse.getStatusCode().is2xxSuccessful()
                                    ? httpResponse.getBody().readNBytes(MAX_ZIP_BYTES + 1) : new byte[0]));
        } catch (RestClientException exception) {
            // Spring의 전송 예외에는 인증키가 포함된 요청 URL이 들어갈 수 있습니다.
            throw new IOException("Open DART 고유번호 응답을 읽지 못했습니다.");
        }

        if (response.status() < 200 || response.status() >= 300) {
            throw new IOException("Open DART 고유번호 HTTP 오류: " + response.status());
        }

        if (response.body().length > MAX_ZIP_BYTES) {
            throw new IOException("Open DART 고유번호 ZIP 크기 제한을 초과했습니다.");
        }

        String errorStatus = DartCorpCodeParser.readErrorStatus(response.body());

        if (errorStatus != null) {
            throw new IOException("Open DART 고유번호 ZIP 응답 오류: " + errorStatus);
        }

        return response.body();
    }

    private static RestClient createRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(30_000);
        return RestClient.builder().baseUrl("https://opendart.fss.or.kr").requestFactory(factory).build();
    }

    private record ArchiveResponse(int status, byte[] body) {
    }
}
