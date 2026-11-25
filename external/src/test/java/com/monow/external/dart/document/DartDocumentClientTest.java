package com.monow.external.dart.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DartDocumentClientTest {

    private static final String RECEIPT_NUMBER = "20260102000001";
    private static final String REQUEST_URL =
            "https://dart.example/api/document.xml?crtfc_key=test-key&rcept_no=" + RECEIPT_NUMBER;

    private MockRestServiceServer server;
    private DartDocumentClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://dart.example");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DartDocumentClient(builder.build(), "test-key");
    }

    @Test
    @DisplayName("[성공] - 접수번호로 원본 파일의 바이너리를 요청한다")
    void givenReceiptNumber_whenFetchDocument_thenReturnResponseBytes() throws IOException {
        // Given
        byte[] body = {'P', 'K', 3, 4, 1};
        server.expect(requestTo(REQUEST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_OCTET_STREAM));

        // When
        assertThat(client.fetchDocument(RECEIPT_NUMBER))
                // Then
                .containsExactly(body);

        server.verify();
    }

    @Test
    @DisplayName("[실패] - DART 오류 XML의 상태 코드를 전달한다")
    void givenErrorXml_whenFetchDocument_thenReportDartStatus() {
        // Given
        server.expect(requestTo(REQUEST_URL))
                .andRespond(withSuccess("<result><status>014</status></result>", MediaType.APPLICATION_XML));

        // When
        assertThatThrownBy(() -> client.fetchDocument(RECEIPT_NUMBER))
                // Then
                .isInstanceOf(IOException.class)
                .hasMessageContaining("014")
                .hasMessageNotContaining("test-key");

        server.verify();
    }

    @Test
    @DisplayName("[실패] - HTTP 오류에서 상태 코드를 전달한다")
    void givenHttpError_whenFetchDocument_thenReportHttpStatus() {
        // Given
        server.expect(requestTo(REQUEST_URL)).andRespond(withServerError());

        // When
        assertThatThrownBy(() -> client.fetchDocument(RECEIPT_NUMBER))
                // Then
                .isInstanceOf(IOException.class)
                .hasMessageContaining("500")
                .hasMessageNotContaining("test-key");

        server.verify();
    }

    @Test
    @DisplayName("[실패] - 잘못된 접수번호로 외부 호출을 하지 않는다")
    void givenInvalidReceiptNumber_whenFetchDocument_thenRejectBeforeRequest() {
        // Given
        String invalidReceiptNumber = "../document";

        // When
        assertThatThrownBy(() -> client.fetchDocument(invalidReceiptNumber))
                // Then
                .isInstanceOf(IllegalArgumentException.class);

        server.verify();
    }
}
