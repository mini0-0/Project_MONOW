package com.monow.external.dart.disclosure;

import com.monow.external.dart.disclosure.dto.response.DartDisclosureItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DartDisclosureClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://dart.example");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final DartDisclosureClient client = new DartDisclosureClient(builder.build(), "test-key");
    private final LocalDate date = LocalDate.of(2026, 10, 1);

    @Test
    @DisplayName("[성공] - 기업코드와 기간을 전달하고 모든 페이지를 반환한다")
    void givenMultiplePages_whenFetchDisclosures_thenReturnAllItems() throws IOException {
        // Given
        expectPage(1, pageBody(1, "20261001000001"));
        expectPage(2, pageBody(2, "20261001000002"));

        // When
        assertThat(client.fetchDisclosures("00126380", date, date))
                // Then
                .containsExactly(
                        new DartDisclosureItem("20261001000001", "00126380", "보고서", "20261001", "제출인"),
                        new DartDisclosureItem("20261001000002", "00126380", "보고서", "20261001", "제출인"));
        server.verify();
    }

    @Test
    @DisplayName("[성공] - 첫 페이지의 조회 결과 없음은 빈 목록으로 반환한다")
    void givenNoData_whenFetchDisclosures_thenReturnEmptyList() throws IOException {
        // Given
        expectPage(1, "{\"status\":\"013\"}");

        // When
        assertThat(client.fetchDisclosures("00126380", date, date))
                // Then
                .isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("[실패] - 두 번째 페이지에서 요청 제한이 발생하면 부분 목록을 반환하지 않는다")
    void givenSecondPageFailure_whenFetchDisclosures_thenRejectPartialResponse() {
        // Given
        expectPage(1, pageBody(1, "20261001000001"));
        expectPage(2, "{\"status\":\"020\"}");

        // When
        assertThatThrownBy(() -> client.fetchDisclosures("00126380", date, date))
                // Then
                .isInstanceOf(IOException.class).hasMessageContaining("020");
        server.verify();
    }

    private void expectPage(int page, String body) {
        server.expect(queryParam("page_no", Integer.toString(page)))
                .andExpect(queryParam("corp_code", "00126380"))
                .andExpect(queryParam("bgn_de", "20261001"))
                .andExpect(queryParam("end_de", "20261001"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private static String pageBody(int page, String receiptNumber) {
        return """
                {"status":"000","page_no":%d,"total_page":2,"list":[
                  {"rcept_no":"%s","corp_code":"00126380","report_nm":"보고서",
                   "rcept_dt":"20261001","flr_nm":"제출인"}
                ]}
                """.formatted(page, receiptNumber);
    }
}
