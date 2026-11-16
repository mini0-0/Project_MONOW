package com.monow.external.navernews.dto.request;

public record NaverNewsSearchRequest(
        String query,
        // 한 번의 요청으로 받고 싶은 뉴스 개수
        Integer display,
        // 검색 결과에서 몇 번째 항목부터 가져올지
        Integer start,
        String sort
) {
    public NaverNewsSearchRequest {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어는 필수입니다.");
        }

        display = display == null ? 10 : display;
        start = start == null ? 1 : start;
        sort = sort == null ? "sim" : sort;

        if (display < 1 || display > 100) {
            throw new IllegalArgumentException("display는 1 이상 100 이하여야 합니다.");
        }

        if (start < 1 || start > 1000) {
            throw new IllegalArgumentException("start는 1 이상 1000 이하여야 합니다.");
        }

        if (!"sim".equals(sort) && !"date".equals(sort)) {
            throw new IllegalArgumentException("sort는 sim 또는 date여야 합니다.");
        }
    }

    public NaverNewsSearchRequest(String query) {
        this(query, null, null, null);
    }
}
