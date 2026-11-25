package com.monow.external.dart.corpcode;

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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DartCorpCodeClientTest {

    private static final String URL = "https://dart.example/api/corpCode.xml?crtfc_key=test-key";

    private MockRestServiceServer server;
    private DartCorpCodeClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://dart.example");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DartCorpCodeClient(builder.build(), "test-key");
    }

    @Test
    @DisplayName("[성공] - 고유번호 API를 한 번 호출하고 응답 바이트를 반환한다")
    void givenArchiveResponse_whenFetchArchive_thenReturnOriginalBytes() throws IOException {
        // Given
        byte[] body = {'P', 'K', 3, 4};
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_OCTET_STREAM));

        // When
        assertThat(client.fetchArchive())
                // Then
                .containsExactly(body);
        server.verify();
    }

    @Test
    @DisplayName("[실패] - 오류 XML의 Open DART 상태 코드를 전달한다")
    void givenErrorResponse_whenFetchCorpCodes_thenReportDartStatus() {
        // Given
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "<result><status>020</status></result>", MediaType.APPLICATION_XML));

        // When
        assertThatThrownBy(client::fetchArchive)
                // Then
                .isInstanceOf(IOException.class)
                .hasMessageContaining("020").hasMessageNotContaining("test-key");
        server.verify();
    }

}
