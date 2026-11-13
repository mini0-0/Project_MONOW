package com.monow.external.kis.stock.client;

import com.monow.external.kis.config.KisProperties;
import com.monow.external.kis.exception.MinuteStockPriceRetryableException;
import com.monow.external.kis.stock.dto.request.KisMinutePriceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KisMinutePriceClientTest {
    private MockRestServiceServer server;
    private KisMinutePriceClient client;
    private final KisMinutePriceRequest request = new KisMinutePriceRequest("005930",
            LocalDate.of(2026, 11, 11), LocalTime.of(15, 30));

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://kis.example");
        server = MockRestServiceServer.bindTo(builder).build();
        KisProperties properties = new KisProperties();
        properties.setAppKey("test-key");
        properties.setAppSecret("test-secret");
        client = new KisMinutePriceClient(properties, builder.build());
    }

    @Test
    @DisplayName("[성공] - UN 통합 시장과 검색 조건 및 인증 헤더를 보내고 응답을 변환")
    void givenValidResponse_whenFetch_thenMapsPricesAndSendsParameters() {
        server.expect(method(HttpMethod.GET))
                .andExpect(queryParam("FID_INPUT_ISCD", "005930"))
                .andExpect(queryParam("FID_COND_MRKT_DIV_CODE", "UN"))
                .andExpect(queryParam("FID_INPUT_DATE_1", "20261111"))
                .andExpect(queryParam("FID_INPUT_HOUR_1", "153000"))
                .andExpect(queryParam("FID_PW_DATA_INCU_YN", "N"))
                .andExpect(header("authorization", "Bearer token"))
                .andExpect(header("appkey", "test-key"))
                .andExpect(header("appsecret", "test-secret"))
                .andExpect(header("tr_id", "FHKST03010230"))
                .andRespond(withSuccess("""
                        {"rt_cd":"0","msg_cd":"OK","msg1":"success","output1":{"extra":1},
                         "output2":[{"stck_bsop_date":"20261111","stck_cntg_hour":"153000",
                         "stck_oprc":"70000","stck_hgpr":"70200","stck_lwpr":"69900",
                         "stck_prpr":"70100","cntg_vol":"1234","unknown":"ignored"}]}
                        """, MediaType.APPLICATION_JSON));
        var response = client.fetchMinutePrice("token", request);
        assertThat(response.output()).hasSize(1);
        assertThat(response.output().getFirst().volume()).isEqualTo("1234");
        server.verify();
    }

    @Test
    @DisplayName("[성공] - 빈 분봉 목록을 그대로 반환")
    void givenEmptyPrices_whenFetch_thenReturnsEmptyList() {
        server.expect(anything()).andRespond(withSuccess("{\"rt_cd\":\"0\",\"output2\":[]}", MediaType.APPLICATION_JSON));
        assertThat(client.fetchMinutePrice("token", request).output()).isEmpty();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 503})
    @DisplayName("[실패] - 서버 오류와 호출 제한 오류를 재시도 가능 예외로 변환")
    void givenTemporaryHttpError_whenFetch_thenThrowsRetryableException(int status) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThatThrownBy(() -> client.fetchMinutePrice("token", request))
                .isInstanceOf(MinuteStockPriceRetryableException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    @DisplayName("[실패] - 인증과 요청 오류를 자동 재시도 예외로 변환하지 않음")
    void givenPermanentHttpError_whenFetch_thenPropagatesHttpException(int status) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThatThrownBy(() -> client.fetchMinutePrice("token", request))
                .isInstanceOf(RestClientResponseException.class);
        server.verify();
    }

    @Test
    @DisplayName("[실패] - 네트워크 오류를 재시도 가능 예외로 변환")
    void givenNetworkFailure_whenFetch_thenThrowsRetryableException() {
        server.expect(anything()).andRespond(withException(new IOException("test network failure")));
        assertThatThrownBy(() -> client.fetchMinutePrice("token", request))
                .isInstanceOf(MinuteStockPriceRetryableException.class);
        server.verify();
    }

    @Test
    @DisplayName("[실패] - 업무 오류와 응답 누락을 거부")
    void givenBusinessError_whenFetch_thenFailsWithoutExposingResponse() {
        server.expect(anything()).andRespond(withSuccess(
                "{\"rt_cd\":\"1\",\"msg1\":\"sensitive-response\",\"output2\":[]}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.fetchMinutePrice("token", request))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("sensitive-response");
        server.verify();
    }

    @Test
    @DisplayName("[실패] - 정상 코드여도 누락된 분봉 목록을 거부")
    void givenMissingOutput_whenFetch_thenFails() {
        server.expect(anything()).andRespond(withSuccess("{\"rt_cd\":\"0\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.fetchMinutePrice("token", request)).isInstanceOf(IllegalStateException.class);
        server.verify();
    }
}
