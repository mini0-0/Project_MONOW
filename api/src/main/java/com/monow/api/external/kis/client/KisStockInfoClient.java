package com.monow.api.external.kis.client;

import com.monow.api.external.kis.config.KisProperties;
import com.monow.api.external.kis.dto.response.KisStockInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KisStockInfoClient {

    private static final String STOCK_INFO_TR_ID = "CTPF1604R";

    private static final String DOMESTIC_STOCK_PRODUCT_TYPE_CODE = "300";

    private final KisProperties kisProperties;

    public KisStockInfoResponse fetchStockInfo(String accessToken, String stockCode) {
        RestClient restClient = RestClient.builder()
                .baseUrl(kisProperties.getBaseUrl())
                .build();

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/search-info")
                        .queryParam("PDNO", stockCode)
                        .queryParam("PRDT_TYPE_CD", DOMESTIC_STOCK_PRODUCT_TYPE_CODE)
                        .build()
                )
                .header("authorization", "Bearer " + accessToken)
                .header("appkey", kisProperties.getAppKey())
                .header("appsecret", kisProperties.getAppSecret())
                .header("tr_id", STOCK_INFO_TR_ID)
                .retrieve()
                .body(KisStockInfoResponse.class);
    }
}
