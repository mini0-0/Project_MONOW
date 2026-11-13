package com.monow.batch.stock.minuteprice.job;

import com.monow.external.kis.stock.client.KisMinutePriceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class MinutePriceConfigurationTest {
    @Test
    @DisplayName("[성공] - 명시적으로 활성화하지 않으면 분봉 Client와 Job을 등록하지 않음")
    void givenDefaultConfiguration_whenStartContext_thenDoesNotRegisterCollectionOrMaintenance() {
        try (var context = new AnnotationConfigApplicationContext(
                MinuteStockPriceJobConfig.class, MinutePricePartitionJobConfig.class)) {
            assertThat(context.getBeansOfType(Job.class)).isEmpty();
            assertThat(context.getBeansOfType(KisMinutePriceClient.class)).isEmpty();
        }
    }
}
