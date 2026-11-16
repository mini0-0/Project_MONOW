package com.monow.external.navernews.config;

import com.monow.external.navernews.client.NaverNewsClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NaverNewsConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(NaverNewsConfig.class);

    @Test
    @DisplayName("[성공] - 기본 비활성 상태에서는 인증 정보 없이 Client를 등록하지 않음")
    void givenDisabledFeature_whenStartContext_thenDoesNotRegisterClient() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(NaverNewsClient.class);
            assertThat(context).doesNotHaveBean(NaverNewsProperties.class);
        });
    }

    @Test
    @DisplayName("[성공] - 환경변수의 인증 정보를 설정 객체에 연결하고 Client 등록")
    void givenEnvironmentCredentials_whenStartContext_thenBindsPropertiesAndRegistersClient() {
        runner.withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("test-environment", Map.of(
                        "NAVER_NEWS_ENABLED", "true",
                        "NAVER_NEWS_CLIENT_ID", "test-id",
                        "NAVER_NEWS_CLIENT_SECRET", "test-secret"))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(NaverNewsClient.class);
                    assertThat(context).hasSingleBean(NaverNewsProperties.class);
                    assertThat(context.getBean(NaverNewsProperties.class).getClientId()).isEqualTo("test-id");
                    assertThat(context.getBean(NaverNewsProperties.class).getClientSecret()).isEqualTo("test-secret");
                });
    }

    @Test
    @DisplayName("[실패] - 인증 정보 누락 시 Bean 생성 실패")
    void rejectMissingCredentials() {
        runner.withPropertyValues(
                "naver.news.enabled=true",
                "naver.news.client-id=",
                "naver.news.client-secret="
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalArgumentException.class);
        });
    }
}
