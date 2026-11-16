package com.monow.external.navernews.config;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public class NaverNewsProperties {

    private final String clientId;

    private final String clientSecret;

    public NaverNewsProperties(String clientId, String clientSecret) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new IllegalArgumentException("네이버 뉴스 인증 정보가 필요합니다.");
        }

        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
}
