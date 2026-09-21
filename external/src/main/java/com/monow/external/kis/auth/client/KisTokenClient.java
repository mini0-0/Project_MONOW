package com.monow.external.kis.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KisTokenClient {

    private final KisProperties kisProperties;

    public KisTokenResponse issueToken() {
        KisTokenRequest request = KisTokenRequest.of(
                kisProperties.getAppKey(),
                kisProperties.getAppSecret()
        );

        RestClient restClient = RestClient.builder()
                .baseUrl(kisProperties.getBaseUrl())
                .build();

        return restClient.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(KisTokenResponse.class);
    }
}
