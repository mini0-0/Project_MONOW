package com.monow.api.external.kis.client;

import com.monow.api.external.kis.config.KisProperties;
import com.monow.api.external.kis.dto.response.KisCurrentPriceResponse;
import com.monow.api.external.kis.type.CurrentPriceMarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KisCurrentPriceClient {

    private final static String CURRENT_PRICE_TR_ID = "FHKST01010100";

    private final KisProperties kisProperties;

    public KisCurrentPriceResponse fetchCurrentPrice(String accessToken, String stockCode, CurrentPriceMarketType marketType) {
        RestClient restClient = RestClient.builder()
                .baseUrl(kisProperties.getBaseUrl())
                .build();

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", marketType.getKisCode())
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .queryParam("custtype", "P")
                        .build()
                )
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", kisProperties.getAppKey())
                .header("appsecret", kisProperties.getAppSecret())
                .header("tr_id", CURRENT_PRICE_TR_ID)
                .retrieve()
                .body(KisCurrentPriceResponse.class);

    }
}
