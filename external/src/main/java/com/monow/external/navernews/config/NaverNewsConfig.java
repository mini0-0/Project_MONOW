package com.monow.external.navernews.config;

import com.monow.external.navernews.client.NaverNewsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "naver.news", name = "enabled", havingValue = "true")
public class NaverNewsConfig {

    @Bean
    public NaverNewsProperties naverNewsProperties(
            @Value("${naver.news.client-id}") String clientId,
            @Value("${naver.news.client-secret}") String clientSecret
    ) {
        return new NaverNewsProperties(clientId, clientSecret);
    }

    @Bean
    public NaverNewsClient naverNewsClient(NaverNewsProperties properties) {
        return new NaverNewsClient(properties);
    }
}