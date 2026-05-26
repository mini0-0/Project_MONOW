package com.monow.api.external.kis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "kis")
public class KisProperties {

    private String baseUrl;

    private String appKey;

    private String appSecret;



}
