package com.monow.external.kis.stock.client;

import com.monow.external.kis.config.KisProperties;
import com.monow.external.kis.exception.MinuteStockPriceRetryableException;
import com.monow.external.kis.stock.dto.request.KisMinutePriceRequest;
import com.monow.external.kis.stock.dto.response.KisMinutePriceResponse;
import com.monow.external.kis.type.CurrentPriceMarketType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.format.DateTimeFormatter;

public class KisMinutePriceClient {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private final KisProperties properties;
    private final RestClient restClient;

    public KisMinutePriceClient(KisProperties properties) {
        this(properties, createRestClient(properties));
    }

    KisMinutePriceClient(KisProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    private static RestClient createRestClient(KisProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        return RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
    }

    public KisMinutePriceResponse fetchMinutePrice(String accessToken, KisMinutePriceRequest request) {
        try {
            KisMinutePriceResponse response = restClient.get()
                    .uri(builder -> builder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice")
                            .queryParam("FID_COND_MRKT_DIV_CODE", CurrentPriceMarketType.INTEGRATED.getKisCode())
                            .queryParam("FID_INPUT_ISCD", request.stockCode())
                            .queryParam("FID_INPUT_DATE_1", request.tradeDate().format(DateTimeFormatter.BASIC_ISO_DATE))
                            .queryParam("FID_INPUT_HOUR_1", request.inputTime().format(TIME_FORMAT))
                            .queryParam("FID_PW_DATA_INCU_YN", "N")
                            .queryParam("FID_FAKE_TICK_INCU_YN", "")
                            .build())
                    .header("authorization", "Bearer " + accessToken)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", "FHKST03010230")
                    .retrieve().body(KisMinutePriceResponse.class);

            if (response == null || !"0".equals(response.rtCd()) || response.output() == null) {
                // 외부 응답 본문과 인증 헤더를 예외 메시지에 포함하지 않는다.
                throw new IllegalStateException("KIS 분봉 업무 오류 또는 응답 누락");
            }
            return response;

        } catch (ResourceAccessException exception) {
            throw new MinuteStockPriceRetryableException("KIS 분봉 네트워크 일시 오류", exception);
        }
        catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is5xxServerError() || exception.getStatusCode().value() == 429) {
                throw new MinuteStockPriceRetryableException("KIS 분봉 서버 또는 호출 제한 오류", exception);
            }
            throw exception;
        }
    }
}
