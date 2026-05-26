package com.monow.api.external.kis.client;

import com.monow.api.external.kis.config.KisProperties;
import com.monow.api.external.kis.dto.response.KisDailyPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KisDailyPriceClient {

    private static final String DAILY_PRICE_TR_ID = "FHKST01010400";

    private final KisProperties kisProperties;


    public KisDailyPriceResponse fetchDailyPrice(String accessToken, String stockCode) {
        RestClient restClient = RestClient.builder()
                .baseUrl(kisProperties.getBaseUrl())
                .build();

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .queryParam("FID_ORG_ADJ_PRC", "0")
                        .build()
                )
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", kisProperties.getAppKey())
                .header("appsecret", kisProperties.getAppSecret())
                .header("tr_id", DAILY_PRICE_TR_ID)
                .retrieve()
                .body(KisDailyPriceResponse.class);

    }
}
