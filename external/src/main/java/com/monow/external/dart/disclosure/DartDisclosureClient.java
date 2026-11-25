package com.monow.external.dart.disclosure;

import com.monow.external.dart.disclosure.dto.response.DartDisclosureItem;
import com.monow.external.dart.disclosure.dto.response.DartDisclosurePageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class DartDisclosureClient {

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public DartDisclosureClient(@Value("${OPEN_DART_API_KEY:}") String apiKey) {
        this(createRestClient(), apiKey);
    }

    DartDisclosureClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public List<DartDisclosureItem> fetchDisclosures(String corpCode, LocalDate from, LocalDate to) throws IOException {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OPEN_DART_API_KEY가 설정되지 않았습니다.");
        }

        var items = new ArrayList<DartDisclosureItem>();
        int totalPages;
        int page = 1;

        do {
            DartDisclosurePageResponse response = fetchPage(corpCode, from, to, page);
            if (response != null && "013".equals(response.status()) && page == 1) {
                return List.of();
            }
            if (response == null || !"000".equals(response.status())) {
                String status = response == null ? null : response.status();
                throw new IOException("Open DART 공시 목록 오류: "
                        + (status != null && status.matches("[0-9]{3}") ? status : "확인 불가"));
            }
            if (response.pageNumber() != page || response.totalPages() < page
                    || response.items() == null || response.items().isEmpty() || response.items().contains(null)) {
                throw new IOException("Open DART 공시 목록 페이지 응답이 올바르지 않습니다.");
            }

            items.addAll(response.items());
            totalPages = response.totalPages();
            page++;
        } while (page <= totalPages);

        return List.copyOf(items);
    }

    private DartDisclosurePageResponse fetchPage(String corpCode, LocalDate from, LocalDate to, int page)
            throws IOException {
        try {
            return restClient.get()
                    .uri(builder -> builder.path("/api/list.json")
                            .queryParam("crtfc_key", "{key}")
                            .queryParam("corp_code", "{corpCode}")
                            .queryParam("bgn_de", from.format(DateTimeFormatter.BASIC_ISO_DATE))
                            .queryParam("end_de", to.format(DateTimeFormatter.BASIC_ISO_DATE))
                            .queryParam("last_reprt_at", "N")
                            .queryParam("page_no", page)
                            .queryParam("page_count", 100)
                            .build(apiKey, corpCode))
                    .retrieve().body(DartDisclosurePageResponse.class);
        } catch (RestClientException exception) {
            // 요청 URL을 포함할 수 있는 전송 예외에서 인증키가 노출되지 않도록 합니다.
            throw new IOException("Open DART 공시 목록 요청에 실패했습니다. 페이지: " + page);
        }
    }

    private static RestClient createRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(30_000);
        return RestClient.builder().baseUrl("https://opendart.fss.or.kr").requestFactory(factory).build();
    }
}
