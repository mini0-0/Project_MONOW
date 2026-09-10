package com.monow.api.external.kis.client;

import com.monow.api.external.kis.application.KisAccessTokenProvider;
import com.monow.api.external.kis.config.KisProperties;
import com.monow.api.external.kis.dto.response.KisTopViewItem;
import com.monow.api.external.kis.dto.response.KisTopViewResponse;
import com.monow.global.error.exception.BusinessException;
import com.monow.global.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KisTopViewClient {

    private static final String RANKING_TOP_VIEW ="HHMCM000100C0";

    private final KisAccessTokenProvider kisAccessTokenProvider;

    private final KisProperties kisProperties;


    public List<KisTopViewItem> fetchTopViewStocks() {
        RestClient restClient = RestClient.builder()
                .baseUrl(kisProperties.getBaseUrl())
                .build();

        String accessToken = kisAccessTokenProvider.getAccessToken();

         KisTopViewResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/ranking/hts-top-view")
                        .build()
                )
                 .header("authorization", "Bearer " + accessToken)
                 .header("appkey", kisProperties.getAppKey())
                 .header("appsecret", kisProperties.getAppSecret())
                 .header("tr_id", RANKING_TOP_VIEW)
                 .header("custtype", "P")
                 .retrieve()
                 .body(KisTopViewResponse.class);

        if (response == null) {
            throw new BusinessException(ErrorCode.KIS_TOP_VIEW_INVALID_RESPONSE);
        }

        if (!"0".equals(response.rtCd())){
            throw new BusinessException(ErrorCode.KIS_TOP_VIEW_API_FAILED);
        }

        if (response.output() == null) {
            throw new BusinessException(ErrorCode.KIS_TOP_VIEW_INVALID_RESPONSE);
        }

        return response.output();
    }
}
