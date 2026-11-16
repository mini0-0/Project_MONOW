package com.monow.external.navernews.client;

import com.monow.external.navernews.config.NaverNewsProperties;
import com.monow.external.navernews.dto.request.NaverNewsSearchRequest;
import com.monow.external.navernews.exception.NaverNewsApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class NaverNewsClientTest {

    private MockRestServiceServer server;
    private NaverNewsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://naverapihub.apigw.ntruss.com");
        server = MockRestServiceServer.bindTo(builder).build();
        NaverNewsProperties properties = new NaverNewsProperties("test-id", "test-secret");

        client = new NaverNewsClient(properties, builder.build());
    }

    @Test
    @DisplayName("[성공] - API HUB 주소와 인증 및 인코딩된 검색 조건을 전달하고 추가 필드를 허용")
    void givenValidResponse_whenSearch_thenSendsEncodedParametersAndMapsNews() {
        server.expect(method(HttpMethod.GET))
                .andExpect(request -> {
                    var uri = request.getURI();
                    assertThat(uri.getHost()).isEqualTo("naverapihub.apigw.ntruss.com");
                    assertThat(uri.getPath()).isEqualTo("/search/v1/news");
                    assertThat(uri.getRawQuery()).contains(
                            "query=%EC%82%BC%EC%84%B1%EC%A0%84%EC%9E%90%20%26%20HBM%2B");
                })
                .andExpect(queryParam("display", "2"))
                .andExpect(queryParam("start", "11"))
                .andExpect(queryParam("sort", "date"))
                .andExpect(queryParam("format", "json"))
                .andExpect(header("X-NCP-APIGW-API-KEY-ID", "test-id"))
                .andExpect(header("X-NCP-APIGW-API-KEY", "test-secret"))
                .andRespond(withSuccess(validResponse(), MediaType.APPLICATION_JSON));

        var response = client.search(new NaverNewsSearchRequest("삼성전자 & HBM+", 2, 11, "date"));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().title()).isEqualTo("<b>삼성전자</b> 관련 가상 기사");
        assertThat(response.items().getFirst().originalLink()).isEqualTo("https://example.com/news/1");
        assertThat(response.items().getFirst().description()).isEqualTo("테스트용 요약");
        server.verify();
    }

    @Test
    @DisplayName("[성공] - 선택 조건을 생략하면 기본값으로 요청")
    void givenOnlyQuery_whenSearch_thenUsesDefaultParameters() {
        server.expect(queryParam("display", "10"))
                .andExpect(queryParam("start", "1"))
                .andExpect(queryParam("sort", "sim"))
                .andRespond(withSuccess(validResponse(), MediaType.APPLICATION_JSON));

        client.search(new NaverNewsSearchRequest("삼성전자"));

        server.verify();
    }

    @Test
    @DisplayName("[성공] - 빈 검색 결과를 정상 반환")
    void givenEmptyResults_whenSearch_thenReturnsEmptyList() {
        server.expect(anything()).andRespond(withSuccess("""
                {"lastBuildDate":"Thu, 11 Jun 2026 19:03:00 +0900",
                 "total":0,"start":1,"display":0,"items":[]}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.search(new NaverNewsSearchRequest("검색어")).items()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("[성공] - 원문 주소가 없어도 네이버 주소가 있으면 허용")
    void givenOnlyNaverLink_whenSearch_thenReturnsNews() {
        server.expect(anything()).andRespond(withSuccess(validResponse().replace(
                "\"originallink\": \"https://example.com/news/1\"", "\"originallink\": null"), MediaType.APPLICATION_JSON));

        assertThat(client.search(new NaverNewsSearchRequest("삼성전자")).items()).hasSize(1);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 429, 500})
    @DisplayName("[실패] - HTTP 오류 상태를 보존하고 오류 본문은 노출하지 않음")
    void givenHttpError_whenSearch_thenPreservesStatusWithoutLeakingBody(int status) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status))
                .body("test-secret: 외부 오류 본문").contentType(MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.search(new NaverNewsSearchRequest("삼성전자")))
                .isInstanceOfSatisfying(NaverNewsApiException.class,
                        exception -> assertThat(exception.getHttpStatus()).isEqualTo(status))
                .hasMessageNotContaining("test-secret").hasNoCause();
        server.verify();
    }

    @Test
    @DisplayName("[실패] - 네트워크 오류는 HTTP 상태 미확보로 구분")
    void givenNetworkError_whenSearch_thenThrowsConnectionException() {
        server.expect(anything()).andRespond(withException(new IOException("테스트 연결 실패")));

        assertThatThrownBy(() -> client.search(new NaverNewsSearchRequest("삼성전자")))
                .isInstanceOfSatisfying(NaverNewsApiException.class,
                        exception -> assertThat(exception.getHttpStatus()).isNull());
        server.verify();
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    @DisplayName("[실패] - 누락되거나 잘못된 응답 데이터를 거부")
    void givenInvalidResponse_whenSearch_thenRejectsResponse(String body) {
        server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search(new NaverNewsSearchRequest("삼성전자")))
                .isInstanceOf(NaverNewsApiException.class);
        server.verify();
    }

    @Test
    @DisplayName("[실패] - 인증 정보가 없으면 설정 객체 생성을 거부")
    void givenMissingCredentials_whenCreateProperties_thenRejectsConfiguration() {
        assertThatThrownBy(() -> new NaverNewsProperties(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<String> invalidResponses() {
        return Stream.of("", "{}", "{invalid-json",
                validResponse().replace("\"total\": 1", "\"total\": -1"),
                validResponse().replace("\"start\": 1", "\"start\": 0"),
                validResponse().replace("\"display\": 1", "\"display\": 101"),
                validResponse().replace("\"items\": [", "\"items\": null, \"unused\": ["),
                validResponse().replace("Thu, 11 Jun 2026 19:03:00 +0900", "invalid-date"),
                validResponse().replace("Thu, 11 Jun 2026 18:34:00 +0900", "invalid-date"),
                validResponse().replace("<b>삼성전자</b> 관련 가상 기사", ""),
                validResponse().replace("https://example.com/news/1", ""));
    }

    private static String validResponse() {
        return """
                {
                  "lastBuildDate": "Thu, 11 Jun 2026 19:03:00 +0900",
                  "total": 1,
                  "start": 1,
                  "display": 1,
                  "additionalField": "추가 필드",
                  "items": [{
                    "title": "<b>삼성전자</b> 관련 가상 기사",
                    "originallink": "https://example.com/news/1",
                    "link": "https://example.com/news/1",
                    "description": "테스트용 요약",
                    "pubDate": "Thu, 11 Jun 2026 18:34:00 +0900",
                    "additionalField": "추가 필드"
                  }]
                }
                """;
    }
}
