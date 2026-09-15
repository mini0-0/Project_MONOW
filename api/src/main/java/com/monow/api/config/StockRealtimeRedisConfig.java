package com.monow.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monow.api.stock.realtime.dto.response.StockRealtimePriceResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class StockRealtimeRedisConfig {

    @Bean
    public RedisTemplate<String, StockRealtimePriceResponse> stockRealtimePriceRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {

        RedisTemplate<String, StockRealtimePriceResponse> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(redisConnectionFactory);


        StringRedisSerializer keySerializer = new StringRedisSerializer();

        Jackson2JsonRedisSerializer<StockRealtimePriceResponse> valueSerializer = new Jackson2JsonRedisSerializer<>(
                objectMapper,
                StockRealtimePriceResponse.class
        );

        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setValueSerializer(valueSerializer);

        redisTemplate.setHashKeySerializer(keySerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);

        redisTemplate.afterPropertiesSet();

        return redisTemplate;


    }
}
