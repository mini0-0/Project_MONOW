package com.monow.external.navernews.client;

import com.monow.external.navernews.config.NaverNewsProperties;
import com.monow.external.navernews.dto.request.NaverNewsSearchRequest;
import com.monow.external.navernews.dto.response.NaverNewsSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(
        named = "NAVER_NEWS_INTEGRATION_TEST_ENABLED",
        matches = "true"
)
@DisplayName("네이버 뉴스 API 실제 연동 테스트")
class NaverNewsClientIntegrationTest {

    @Test
    @DisplayName("[성공] - 실제 네이버 뉴스 API 검색")
    void searchRealNews() {
        String clientId = System.getenv("NAVER_NEWS_CLIENT_ID");
        String clientSecret = System.getenv("NAVER_NEWS_CLIENT_SECRET");

        assertThat(StringUtils.hasText(clientId)).isTrue();
        assertThat(StringUtils.hasText(clientSecret)).isTrue();

        NaverNewsClient client =
                new NaverNewsClient(new NaverNewsProperties(clientId, clientSecret));

        NaverNewsSearchRequest request =
                new NaverNewsSearchRequest("삼성전자", 5, 1, "date");

        NaverNewsSearchResponse response = client.search(request);

        assertThat(response).isNotNull();
        assertThat(response.items()).isNotEmpty();
        assertThat(response.items().size()).isLessThanOrEqualTo(5);
        assertThat(response.start()).isEqualTo(1);

        System.out.println("전체 검색 결과: " + response.total());
        System.out.println("조회한 뉴스 개수: " + response.items().size());
    }
}
