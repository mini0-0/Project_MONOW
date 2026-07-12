package com.monow.api.external.kis.client;

import com.monow.api.external.kis.config.KisProperties;
import com.monow.api.external.kis.dto.request.KisWebSocketApprovalKeyRequest;
import com.monow.api.external.kis.dto.response.KisWebSocketApprovalKeyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KisWebSocketApprovalKeyClient {

    private static final String APPROVAL_KEY_PATH = "/oauth2/Approval";

    private static final String GRANT_TYPE = "client_credentials";

    private final KisProperties kisProperties;

    public String fetchApprovalKey() {
        KisWebSocketApprovalKeyRequest request = new KisWebSocketApprovalKeyRequest(
                GRANT_TYPE,
                kisProperties.getAppKey(),
                kisProperties.getAppSecret()
        );

        RestClient restClient = RestClient.builder()
                .baseUrl(kisProperties.getBaseUrl())
                .build();

        KisWebSocketApprovalKeyResponse response = restClient.post()
                .uri(APPROVAL_KEY_PATH)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .retrieve()
                .body(KisWebSocketApprovalKeyResponse.class);

        if (response == null) {
            throw new IllegalStateException("KIS WebSocket 접속키 응답이 비어 있습니다.");
        }

        if (response.approvalKey() == null || response.approvalKey().isBlank()) {
            throw new IllegalStateException("KIS WebSocket 접속키가 비어 있습니다.");
        }

        return response.approvalKey();
    }

}
