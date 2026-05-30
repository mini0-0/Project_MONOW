package com.monow.api.external.kis.application;

import com.monow.api.external.kis.client.KisTokenClient;
import com.monow.api.external.kis.dto.response.KisTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class KisAccessTokenProvider {

    private final KisTokenClient kisTokenClient;
    private final Clock clock;

    private String accessToken;

    private LocalDateTime expiredAt;


    public String getAccessToken() {
        if (isValidToken()) {
            return accessToken;
        }

        KisTokenResponse tokenResponse = kisTokenClient.issueToken();

        this.accessToken = tokenResponse.accessToken();
        this.expiredAt = LocalDateTime.now(clock).plusSeconds(tokenResponse.expiresIn());

        return this.accessToken;
    }

    private boolean isValidToken() {
        LocalDateTime nowTime = LocalDateTime.now(clock);

        return accessToken != null
                && expiredAt != null
                && nowTime.isBefore(expiredAt.minusMinutes(1));
    }
}
