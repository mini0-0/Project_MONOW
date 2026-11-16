package com.monow.external.navernews.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class NaverNewsSearchRequestTest {

    @Test
    @DisplayName("[성공] - 생략한 선택 조건에 기본값 적용")
    void givenOnlyQuery_whenCreateRequest_thenAppliesDefaults() {
        var request = new NaverNewsSearchRequest("삼성전자");

        assertThat(request.display()).isEqualTo(10);
        assertThat(request.start()).isEqualTo(1);
        assertThat(request.sort()).isEqualTo("sim");
    }

    @Test
    @DisplayName("[성공] - 검색 조건의 최솟값과 최댓값 허용")
    void givenBoundaryValues_whenCreateRequest_thenAcceptsParameters() {
        assertThat(new NaverNewsSearchRequest("삼성전자", 1, 1, "sim").display()).isEqualTo(1);
        assertThat(new NaverNewsSearchRequest("삼성전자", 100, 1000, "date").start()).isEqualTo(1000);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    @DisplayName("[실패] - 비어 있는 검색어 거부")
    void givenBlankQuery_whenCreateRequest_thenRejectsParameter(String query) {
        assertThatThrownBy(() -> new NaverNewsSearchRequest(query)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    @DisplayName("[실패] - 허용 범위를 벗어난 결과 개수 거부")
    void givenInvalidDisplay_whenCreateRequest_thenRejectsParameter(int display) {
        assertThatThrownBy(() -> new NaverNewsSearchRequest("삼성전자", display, 1, "sim"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1001})
    @DisplayName("[실패] - 허용 범위를 벗어난 검색 시작 위치 거부")
    void givenInvalidStart_whenCreateRequest_thenRejectsParameter(int start) {
        assertThatThrownBy(() -> new NaverNewsSearchRequest("삼성전자", 10, start, "sim"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "DATE", "invalid"})
    @DisplayName("[실패] - 허용하지 않은 정렬 방식 거부")
    void givenInvalidSort_whenCreateRequest_thenRejectsParameter(String sort) {
        assertThatThrownBy(() -> new NaverNewsSearchRequest("삼성전자", 10, 1, sort))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
