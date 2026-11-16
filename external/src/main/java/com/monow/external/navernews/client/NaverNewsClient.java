package com.monow.external.navernews.client;

import com.monow.external.navernews.config.NaverNewsProperties;
import com.monow.external.navernews.dto.request.NaverNewsSearchRequest;
import com.monow.external.navernews.dto.response.NaverNewsSearchResponse;
import com.monow.external.navernews.exception.NaverNewsApiException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class NaverNewsClient {

    private static final String BASE_URL = "https://naverapihub.apigw.ntruss.com";

    private final NaverNewsProperties properties;
    private final RestClient restClient;

    public NaverNewsClient(NaverNewsProperties properties) {
        this(properties, createRestClient());
    }

    NaverNewsClient(NaverNewsProperties properties, RestClient restClient) {
        Objects.requireNonNull(properties, "네이버 뉴스 설정은 필수입니다.");

        if (!StringUtils.hasText(properties.getClientId()) || !StringUtils.hasText(properties.getClientSecret())) {
            throw new IllegalArgumentException("네이버 뉴스 인증 정보가 필요합니다.");
        }

        this.properties = properties;
        this.restClient = Objects.requireNonNull(restClient, "HTTP Client는 필수입니다.");
    }

    private static RestClient createRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);

        return RestClient.builder().baseUrl(BASE_URL).requestFactory(factory).build();
    }

    public NaverNewsSearchResponse search(NaverNewsSearchRequest request) {
        Objects.requireNonNull(request, "검색 요청은 필수입니다.");

        try {
            NaverNewsSearchResponse response = restClient.get()
                    .uri(builder -> builder
                            .path("/search/v1/news")
                            .queryParam("query", "{query}")
                            .queryParam("display", request.display())
                            .queryParam("start", request.start())
                            .queryParam("sort", request.sort())
                            .queryParam("format", "json")
                            .build(request.query()))
                    .header("X-NCP-APIGW-API-KEY-ID", properties.getClientId())
                    .header("X-NCP-APIGW-API-KEY", properties.getClientSecret())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(NaverNewsSearchResponse.class);

            validateResponse(response, request);
            return response;

        } catch (RestClientResponseException exception) {
            // 외부 오류 본문과 원본 예외에 인증 정보가 포함될 가능성을 차단한다.
            throw new NaverNewsApiException(exception.getStatusCode().value(), "네이버 뉴스 API HTTP 오류");

        } catch (ResourceAccessException exception) {
            throw new NaverNewsApiException(null, "네이버 뉴스 API 연결 또는 응답 시간 오류");

        } catch (RestClientException exception) {
            throw new NaverNewsApiException(null, "네이버 뉴스 API 응답 처리 오류");
        }
    }

    private void validateResponse(NaverNewsSearchResponse response, NaverNewsSearchRequest request) {
        if (response == null || response.total() == null || response.total() < 0
                || response.start() == null || response.start() < 1 || response.start() > 1000
                || response.display() == null || response.display() < 0 || response.display() > 100
                || response.items() == null || response.items().size() > request.display()) {
            throw invalidResponse();
        }

        validateDate(response.lastBuildDate());

        for (NaverNewsSearchResponse.Item item : response.items()) {
            if (item == null || !StringUtils.hasText(item.title())
                    || (!StringUtils.hasText(item.originalLink()) && !StringUtils.hasText(item.link()))) {
                throw invalidResponse();
            }

            validateDate(item.pubDate());
        }
    }

    private void validateDate(String value) {
        if (!StringUtils.hasText(value)) {
            throw invalidResponse();
        }

        try {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (DateTimeParseException exception) {
            throw invalidResponse();
        }
    }

    private NaverNewsApiException invalidResponse() {
        return new NaverNewsApiException(200, "네이버 뉴스 API 응답 형식이 올바르지 않습니다.");
    }
}
